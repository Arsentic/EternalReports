package xyz.oribuin.eternalreports.menu

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import xyz.oribuin.eternalreports.EternalReports
import xyz.oribuin.eternalreports.util.FileUtils.createMenuFile
import java.io.File

abstract class Menu(val plugin: EternalReports, private val guiName: String) {
    val menuConfig: FileConfiguration

    init {
        createMenuFile(plugin, guiName)
        menuConfig = YamlConfiguration.loadConfiguration(menuFile)
    }

    fun reload() {
        createMenuFile(plugin, guiName)
        YamlConfiguration.loadConfiguration(menuFile)
    }

    fun getGuiName(): String {
        return guiName.toLowerCase()
    }

    private val menuFile: File
        get() = File("${plugin.dataFolder}${File.separator}menus", getGuiName() + ".yml")
}

