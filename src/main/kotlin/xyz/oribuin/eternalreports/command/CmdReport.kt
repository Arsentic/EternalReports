package xyz.oribuin.eternalreports.command

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import xyz.oribuin.eternalreports.EternalReports
import xyz.oribuin.eternalreports.data.Report
import xyz.oribuin.eternalreports.event.PlayerReportEvent
import xyz.oribuin.eternalreports.manager.ConfigManager
import xyz.oribuin.eternalreports.manager.DataManager
import xyz.oribuin.eternalreports.manager.MessageManager
import xyz.oribuin.eternalreports.manager.ReportManager
import xyz.oribuin.eternalreports.util.HexUtils.colorify
import xyz.oribuin.eternalreports.util.PluginUtils
import xyz.oribuin.eternalreports.util.StringPlaceholders
import java.awt.Color
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

class CmdReport(override val plugin: EternalReports) : OriCommand(plugin, "report") {

    private val cooldowns = mutableMapOf<UUID, Long>()

    override fun executeCommand(sender: CommandSender, args: Array<String>) {
        val msg = plugin.getManager(MessageManager::class)
        val reportManager = plugin.getManager(ReportManager::class)
        val dataManager = plugin.getManager(DataManager::class)

        // Check if sender is player
        if (sender !is Player) {
            msg.sendMessage(sender, "player-only")
            return
        }

        if (cooldowns.containsKey(sender.uniqueId)) {
            val secondsLeft = (cooldowns[sender.uniqueId]
                    ?: return).div(1000).plus(ConfigManager.Setting.COOLDOWN.long).minus(System.currentTimeMillis().div(1000))

            if (secondsLeft > 0) {
                msg.sendMessage(sender, "cooldown", StringPlaceholders.single("cooldown", secondsLeft))
                return
            }
        }

        cooldowns[sender.uniqueId] = System.currentTimeMillis()

        // Check arguments
        if (args.size <= 1) {
            msg.sendMessage(sender, "invalid-arguments")
            return
        }


        // Reported user
        val reported = Bukkit.getPlayer(args[0])?.uniqueId?.let { Bukkit.getOfflinePlayer(it) }

        // Check if reported user is null
        if (reported == null) {
            msg.sendMessage(sender, "invalid-player")
            return
        }

        // Check if the player has permission to bypass report
//        if ((reported.player ?: return).hasPermission("eternalreports.bypass")) {
//            msg.sendMessage(sender, "has-bypass")
//            return
//        }

        // Report reason
        val reason = java.lang.String.join(" ", *args).substring(args[0].length + 1)
        val report = Report(reportManager.reports.size + 1, sender, reported, reason, false, System.currentTimeMillis())

        // Create Placeholders
        val placeholders = StringPlaceholders.builder()
                .addPlaceholder("sender", report.sender.name)
                .addPlaceholder("reported", report.reported.name)
                .addPlaceholder("reason", report.reason)
                .addPlaceholder("report_id", report.id)
                .addPlaceholder("time", PluginUtils.formatTime(report.time))
                .build()

        if (reportManager.reports.contains(report)) {
            msg.sendMessage(sender, "report-exists", placeholders)
            return
        }

        // Send the command sender the report message
        msg.sendMessage(sender, "commands.reported-user", placeholders)

        // Message staff members with alerts
        Bukkit.getOnlinePlayers().stream()
                .filter { staffMember: Player -> staffMember.hasPermission("eternalreports.alerts") && plugin.toggleList.contains(staffMember.uniqueId) }
                .forEach { staffMember: Player ->
                    if (ConfigManager.Setting.ALERT_SETTINGS_SOUND_ENABLED.boolean) {

                        // Why such a long method kotlin?
                        ConfigManager.Setting.ALERT_SETTINGS_SOUND.string.let { Sound.valueOf(it) }.let { staffMember.playSound(staffMember.location, it, ConfigManager.Setting.ALERT_SETTINGS_SOUND_VOLUME.float, 1.toFloat()) }
                    }
                    msg.sendMessage(staffMember, "alerts.user-reported", placeholders)
                }

        dataManager.createReport(sender, reported, reason)
        dataManager.updateReportsMade(sender, dataManager.getReportsMade(sender) + 1)
        reported.player?.let { reported.player?.let { dataManager.getReportsMade(it).plus(1) }?.let { it1 -> dataManager.updateReportsAgainst(it, it1) } }

        Bukkit.getPluginManager().callEvent(PlayerReportEvent(report))

        if (ConfigManager.Setting.USE_WEBHOOK.boolean) {
            this.createReport(sender, report)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): MutableList<String>? {

        val suggestions: MutableList<String> = ArrayList()
        if (args.isEmpty() || args.size == 1) {
            val subCommand = if (args.isEmpty()) "" else args[0]

            val players = mutableListOf<String>()
            Bukkit.getOnlinePlayers().stream().filter { player -> !player.hasMetadata("vanished") }.forEachOrdered { player -> players.add(player.name) }
            players.sortBy { s -> s }

            StringUtil.copyPartialMatches(subCommand, players, suggestions)
            return null
        } else if (args.size == 2) {
            StringUtil.copyPartialMatches(args[1].toLowerCase(), setOf("<reason>"), suggestions)
        } else {
            return null
        }
        return suggestions
    }

    private fun createReport(player: Player, report: Report) {
        try {

            val embedColor: Color = try {
                Color.decode("#FF0000")
            } catch (ex: NumberFormatException) {
                Color.WHITE
            }
            val embedColorRgb = embedColor.rgb and 0xFFFFFF // Strips alpha channel from the Color#decode

            val json = JsonObject()
            val embedJson = JsonObject()
            embedJson.addProperty("title", "Ori's Report Module")
            embedJson.addProperty("description", ("Welcome to Ori's report module, A player has submitted a report ingame, find information about the report here." +
                    "\n" +
                    "\nReport module forked from [EternalReports](https://github.com/Oribuin/EternalReports)"))
            embedJson.addProperty("color", embedColorRgb)

            val embedArray = JsonArray()
            embedArray.add(embedJson)
            json.add("embeds", embedArray)

            val url = URL("https://discordapp.com/api/webhooks/765321095372734484/grFq_QcGOVG4jdNjjRABC7Q-cjVUYB2j52B8DGSIoDiBNafsY1K18SRtXa4gZftW90P2")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"

            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Lil' Ori Test")
            connection.doOutput = true

            connection.outputStream.use { out ->
                out.write(json.toString().toByteArray(StandardCharsets.UTF_8))
                out.flush()
            }

            connection.inputStream.close()
            connection.disconnect()
        } catch (ex: IOException) {
            player.sendMessage(colorify("#B00B1EInvalid URL."))
            ex.printStackTrace()
        }
    }

    override fun addSubCommands() {
        // Unused
    }
}