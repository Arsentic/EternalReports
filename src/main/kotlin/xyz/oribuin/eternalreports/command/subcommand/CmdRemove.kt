package xyz.oribuin.eternalreports.command.subcommand

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import xyz.oribuin.eternalreports.EternalReports
import xyz.oribuin.eternalreports.command.OriCommand
import xyz.oribuin.eternalreports.command.SubCommand
import xyz.oribuin.eternalreports.event.ReportDeleteEvent
import xyz.oribuin.eternalreports.manager.ConfigManager
import xyz.oribuin.eternalreports.manager.DataManager
import xyz.oribuin.eternalreports.manager.MessageManager
import xyz.oribuin.eternalreports.manager.ReportManager
import xyz.oribuin.eternalreports.util.StringPlaceholders

class CmdRemove(val plugin: EternalReports, command: OriCommand) : SubCommand(command, "remove", "delete") {
    private val messageManager = plugin.getManager(MessageManager::class)

    override fun executeArgument(sender: CommandSender, args: Array<String>) {
        // Check permission
        if (!sender.hasPermission("eternalreports.delete")) {
            messageManager.sendMessage(sender, "invalid-permission")
            return
        }

        // Check args
        if (args.size == 1) {
            messageManager.sendMessage(sender, "invalid-arguments")
            return
        }

        // Get reports matching ID
        val reports = plugin.getManager(ReportManager::class).reports.filter { report -> report.id == args[1].toInt() }

        // Check if there aren't any matchingg
        if (reports.isEmpty()) {
            messageManager.sendMessage(sender, "invalid-report")
            return
        }

        // Get report if not null
        val report = reports[0]

        // Placeholders
        val placeholders = StringPlaceholders.builder()
                .addPlaceholder("sender", sender.name)
                .addPlaceholder("reported", report.reported.name)
                .addPlaceholder("reason", report.reason)
                .addPlaceholder("report_id", report.id).build()

        // Send message
        messageManager.sendMessage(sender, "commands.removed-report", placeholders)

        // Message staff members with alerts
        Bukkit.getOnlinePlayers().stream()
                // Check if the players online has permission for alerts and reports are toggled on.
                .filter { staffMember -> staffMember.hasPermission("eternalreports.alerts") && plugin.toggleList.contains(staffMember.uniqueId) }

                // Send alert messages to all 'staff members'
                .forEach { staffMember ->
                    if (ConfigManager.Setting.ALERT_SETTINGS_SOUND_ENABLED.boolean) {
                        staffMember.playSound(staffMember.location, Sound.valueOf(ConfigManager.Setting.ALERT_SETTINGS_SOUND.string), ConfigManager.Setting.ALERT_SETTINGS_SOUND_VOLUME.float, 0f)
                    }

                    // Actually send the message
                    messageManager.sendMessage(staffMember, "alerts.report-deleted", placeholders)
                }


        // Delete report
        plugin.getManager(DataManager::class).deleteReport(report)
        // Call Event
        Bukkit.getPluginManager().callEvent(ReportDeleteEvent(report))
    }

}