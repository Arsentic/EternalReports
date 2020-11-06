package xyz.oribuin.eternalreports.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil
import xyz.oribuin.eternalreports.EternalReports
import xyz.oribuin.eternalreports.command.subcommand.*
import xyz.oribuin.eternalreports.manager.MessageManager
import xyz.oribuin.eternalreports.manager.ReportManager
import xyz.oribuin.eternalreports.menu.ReportsMenu
import xyz.oribuin.eternalreports.util.HexUtils

class CmdReports(override val plugin: EternalReports) : OriCommand(plugin, "reports") {

    companion object {
        var instance: CmdReports? = null
            private set
    }

    init {
        instance = this
    }

    private val subcommands = mutableListOf<SubCommand>()

    private val messageManager = plugin.getManager(MessageManager::class)

    override fun executeCommand(sender: CommandSender, args: Array<String>) {

        for (cmd in subcommands) {
            if (args.isEmpty()) {
                messageManager.sendMessage(sender, "unknown-command")
                break
            }

            if (args.isNotEmpty() && cmd.names.contains(args[0].toLowerCase())) {
                cmd.executeArgument(sender, args)
                break
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): MutableList<String>? {

        val deleteCmdList = listOf("delete", "remove")

        val suggestions: MutableList<String> = ArrayList()
        if (args.isEmpty() || args.size == 1) {
            val subCommand = if (args.isEmpty()) "" else args[0]

            val commands = mutableListOf<String>()

            if (sender.hasPermission("eternalreports.help"))
                commands.add("help")

            if (sender.hasPermission("eternalreports.reload"))
                commands.add("reload")

            if (sender.hasPermission("eternalreports.menu"))
                commands.add("menu")

            if (sender.hasPermission("eternalreports.resolve"))
                commands.add("resolve")

            if (sender.hasPermission("eternalreports.delete")) {
                commands.add("remove")
                commands.add("delete")
            }

            if (sender.hasPermission("eternalreports.toggle")) {
                commands.add("alerts")
                commands.add("toggle")
            }

            StringUtil.copyPartialMatches(subCommand, commands, suggestions)
        } else if (args.size == 2) {
            if (args[0].toLowerCase() == "menu" && sender.hasPermission("eternalreports.menu.other")) {
                val players: MutableList<String> = ArrayList()
                Bukkit.getOnlinePlayers().stream().filter { player -> !player.hasPermission("vanished") }.forEach { player -> players.add(player.name) }

                StringUtil.copyPartialMatches(args[1].toLowerCase(), players, suggestions)

            } else if (args[0].toLowerCase() == "resolve" && sender.hasPermission("eternalreports.resolve") || deleteCmdList.contains(args[0].toLowerCase()) && sender.hasPermission("eternalreports.delete")) {

                val ids: MutableList<String> = ArrayList()
                plugin.getManager(ReportManager::class).reports.stream().forEach { t -> ids.add(t.id.toString()) }

                StringUtil.copyPartialMatches(args[1].toLowerCase(), ids, suggestions)
            }
        } else {
            return null
        }
        return suggestions
    }

    fun getSubCommand(string: String): SubCommand {
        return subcommands.stream().filter { cmd -> cmd.names.contains(string) }.findFirst().get()
    }

    override fun addSubCommands() {
        subcommands.addAll(listOf(CmdHelp(plugin, this), CmdMenu(plugin, this), CmdBook(plugin, this), CmdReload(plugin, this), CmdRemove(plugin, this), CmdResolve(plugin, this), CmdToggle(plugin, this)))
    }

    private fun resolvedFormatted(resolved: Boolean): String? {
        return if (resolved) {
            messageManager.messageConfig.getString("resolve-formatting.is-resolved")?.let { HexUtils.colorify(it) }
        } else {
            messageManager.messageConfig.getString("resolve-formatting.isnt-resolved")?.let { HexUtils.colorify(it) }
        }
    }

}