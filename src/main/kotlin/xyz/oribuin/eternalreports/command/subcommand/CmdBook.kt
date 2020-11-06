package xyz.oribuin.eternalreports.command.subcommand

import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BookMeta
import xyz.oribuin.eternalreports.EternalReports
import xyz.oribuin.eternalreports.command.OriCommand
import xyz.oribuin.eternalreports.command.SubCommand
import xyz.oribuin.eternalreports.util.BookCreator

class CmdBook(private val plugin: EternalReports, command: OriCommand) : SubCommand(command, "book") {

    override fun executeArgument(sender: CommandSender, args: Array<String>) {
        if (sender !is Player) return

        BookCreator.Builder()
                .addBook(listOf("I am a new book"), BookMeta.Generation.ORIGINAL).open(sender)
                .thenOpen(plugin, BookCreator.Book(listOf("I am your second book, third one will show up in 3 seconds!"), null), sender, 5)
                .thenOpen(plugin, BookCreator.Book(listOf("I am your third book!"), null), sender, 10)
                .then(plugin, 15) { sender.health = 0.0; sender.sendMessage("Bye Bye!")}

        ClickEvent.Action.
    }
}