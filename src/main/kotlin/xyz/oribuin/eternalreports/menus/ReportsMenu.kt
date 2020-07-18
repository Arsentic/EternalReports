package xyz.oribuin.eternalreports.menus

import dev.rosewood.guiframework.GuiFactory
import dev.rosewood.guiframework.GuiFramework
import dev.rosewood.guiframework.gui.GuiContainer
import dev.rosewood.guiframework.gui.GuiSize
import dev.rosewood.guiframework.gui.screen.GuiScreen
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import xyz.oribuin.eternalreports.data.Report
import xyz.oribuin.eternalreports.hooks.PlaceholderAPIHook
import xyz.oribuin.eternalreports.utils.HexUtils
import xyz.oribuin.eternalreports.utils.StringPlaceholders
import java.sql.Connection
import java.util.*
import kotlin.collections.ArrayList

class ReportsMenu(private val player: Player?) : Menu("report-menu") {
    private val guiFramework: GuiFramework = GuiFramework.instantiate(plugin)
    private val guiContainer = GuiFactory.createContainer()

    fun openGui() {
        if (isInvalid) buildGui()
        if (player != null) {
            guiContainer.openFor(player)
        }
    }

    private fun buildGui() {
        guiContainer.addScreen(globalReports())
        guiFramework.guiManager.registerGui(guiContainer)
    }

    private fun globalReports(): GuiScreen {
        val guiScreen = GuiFactory.createScreen(guiContainer, GuiSize.ROWS_SIX)
                .setTitle(HexUtils.colorify(this.getValue("menu-name")))

        this.borderSlots().forEach { slot: Int -> guiScreen.addItemStackAt(slot, getItem("border-item")) }

        val reports = mutableListOf<Report>()

        this.plugin.connector.connect { connection: Connection ->
            val query = "SELECT * FROM ${plugin.dataManager.tablePrefix}reports"

            connection.prepareStatement(query).use { statement ->
                val result = statement.executeQuery()
                while (result.next()) {

                    reports.add(Report(
                            Bukkit.getOfflinePlayer(UUID.fromString(result.getString("sender"))), // Sender
                            Bukkit.getOfflinePlayer(UUID.fromString(result.getString("reported"))), // Reported
                            result.getString("reason"), // Reason
                            result.getBoolean("resolved"))) // Is resolved
                }
            }

        }

        guiScreen.setPaginatedSection(GuiFactory.createScreenSection(reportSlots()), reports.size) { _: Int, startIndex: Int, endIndex: Int ->
            val results = GuiFactory.createPageContentsResult()
            for (i in startIndex until endIndex.coerceAtMost(reports.size)) {
                val report = reports[i]


                val placeholders = StringPlaceholders.builder()
                        .addPlaceholder("sender", report.sender.name)
                        .addPlaceholder("reported", report.reported.name)
                        .addPlaceholder("reason", report.reason)
                        .addPlaceholder("resolved", resolvedFormatted(report.isResolved)).build()


                val guiButton = GuiFactory.createButton()
                        .setName(this.format("#C0ffeeReported User: %reported", placeholders))
                        .setLore(this.format("&cReported By: &f%sender%", placeholders),
                                this.format("&cReason: &f%reason%", placeholders),
                                " ",
                                this.format("Resolved: %resolved", placeholders))
                        .setIcon(Material.PLAYER_HEAD) { itemMeta: ItemMeta ->
                            val meta = itemMeta as SkullMeta
                            meta.owningPlayer = report.reported
                        }

                results.addPageContent(guiButton)
            }

            return@setPaginatedSection results
        }

        return guiScreen
    }

    private val isInvalid: Boolean get() = !guiFramework.guiManager.activeGuis.contains(guiContainer)

    private fun borderSlots(): List<Int> {
        val slots: MutableList<Int> = ArrayList()
        for (i in 0..8) slots.add(i)
        run {
            var i = 9
            while (i <= 36) {
                slots.add(i)
                i += 9
            }
        }

        run {
            var i = 17
            while (i <= 44) {
                slots.add(i)
                i += 9
            }
        }

        for (i in 45..53) slots.add(i)
        slots.addAll(listOf(45, 53))
        return slots
    }

    private fun reportSlots(): List<Int> {
        val reportSlots: MutableList<Int> = ArrayList()
        for (i in 10..16) reportSlots.add(i)
        for (i in 19..25) reportSlots.add(i)
        for (i in 28..34) reportSlots.add(i)
        for (i in 37..43) reportSlots.add(i)
        return reportSlots
    }

    // Get value config path formatted
    private fun getValue(configPath: String): String {
        return menuConfig.getString(configPath)?.let { HexUtils.colorify(it) }?.let { PlaceholderAPIHook.apply(player, it) }!!
    }

    private fun format(string: String, placeholders: StringPlaceholders): String {
        return HexUtils.colorify(PlaceholderAPIHook.apply(player, placeholders.apply(string)))
    }

    private fun getItem(configPath: String): ItemStack {
        val itemStack = menuConfig.getString("$configPath.material")?.let { Material.valueOf(it) }?.let { ItemStack(it) }
        val itemMeta = itemStack?.itemMeta ?: return ItemStack(Material.AIR)

        itemMeta.setDisplayName(this.getValue("$configPath.name"))

        val lore: MutableList<String> = ArrayList()
        for (string in menuConfig.getStringList("$configPath.lore"))
            lore.add(this.format(string, StringPlaceholders.empty()))
        itemMeta.lore = lore

        if (menuConfig.getBoolean("$configPath.glowing")) {
            itemMeta.addEnchant(Enchantment.MENDING, 1, true)
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }

        for (itemFlag in menuConfig.getStringList("$configPath.item-flags"))
            itemMeta.addItemFlags(ItemFlag.valueOf(itemFlag))

        itemStack.itemMeta = itemMeta
        return itemStack
    }

    private fun resolvedFormatted(resolved: Boolean): String {
        if (resolved) {
            return this.getValue("resolved-formatting.is-resolved")
        } else {
            return this.getValue("resolved-formatting.isnt-resolved")
        }
    }
}