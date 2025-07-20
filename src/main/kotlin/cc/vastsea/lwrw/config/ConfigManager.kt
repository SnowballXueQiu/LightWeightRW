package cc.vastsea.lwrw.config

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: LightWeightRW) {

    private lateinit var config: FileConfiguration

    fun loadConfig() {
        val configFile = File(plugin.dataFolder, "config.yml")

        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
        }

        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("Configuration loaded successfully!")
    }

    fun getConfig(): FileConfiguration = config

    // 世界相关配置
    fun getResourceWorldName(): String = config.getString("world.name", "resource_world")!!

    fun getWorldSeed(): Long = config.getLong("world.seed", 0L)

    fun isNetherDisabled(): Boolean = config.getBoolean("world.disable-nether", true)

    fun isEndDisabled(): Boolean = config.getBoolean("world.disable-end", true)

    // 传送相关配置
    fun getTeleportRadius(): Int = config.getInt("teleport.radius", 1000)

    fun getTeleportCooldown(): Int = config.getInt("teleport.cooldown", 300)

    fun getMinY(): Int = config.getInt("teleport.min-y", 64)

    fun getMaxY(): Int = config.getInt("teleport.max-y", 120)

    // 重置相关配置
    fun isAutoResetEnabled(): Boolean = config.getBoolean("reset.auto-reset.enabled", true)

    fun getResetTime(): String = config.getString("reset.auto-reset.time", "24:00")!!

    fun getResetWarningTimes(): List<Int> = config.getIntegerList("reset.warning-times")

    fun shouldKickPlayersOnReset(): Boolean = config.getBoolean("reset.kick-players", true)

    // 语言配置
    fun getDefaultLanguage(): String = config.getString("language.default", "en_US")!!

    fun isPerPlayerLanguage(): Boolean = config.getBoolean("language.per-player", true)

    // 调试配置
    fun isDebugEnabled(): Boolean = config.getBoolean("debug", false)
}