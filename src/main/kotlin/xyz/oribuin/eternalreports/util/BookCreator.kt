package xyz.oribuin.eternalreports.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.Plugin
import xyz.oribuin.eternalreports.util.HexUtils.colorify

/**
 * @author Oribuin
 */
class BookCreator {
    private val books = mutableListOf<Book>()

    class Builder {
        private val bookCreator: BookCreator = BookCreator()

        fun addBook(pages: List<String>, generation: BookMeta.Generation?): Book {
            val book = Book(pages, generation)
            bookCreator.books.add(book)
            return book
        }
    }

    data class Book(val pages: List<String>, val generation: BookMeta.Generation?) {
        fun open(vararg players: Player): Book {
            players.forEach { pl -> openAsMinecraftBook(pl, this) }
            return this
        }

        fun open(player: Player): Book {
            openAsMinecraftBook(player, this)
            return this
        }

        fun thenOpen(plugin: Plugin, book: Book, player: Player, seconds: Int): Book {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                openAsMinecraftBook(player, book)
            }, (seconds * 20).toLong())

            return book
        }

        fun then(plugin: Plugin, seconds: Int, runnable: Runnable) {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, (seconds * 20).toLong())
        }

        private fun openAsMinecraftBook(player: Player, book: Book) {
            val item = ItemStack(Material.WRITTEN_BOOK)
            val meta = item.itemMeta as BookMeta

            if (book.generation != null)
                meta.generation = book.generation

            book.pages.forEach { page ->
                meta.addPage(colorify(page))
            }

            meta.author = player.name
            meta.title = player.name

            item.itemMeta = meta

            player.openBook(item)
        }

    }
}