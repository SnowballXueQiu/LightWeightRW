package cc.vastsea.lwrw.managers

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStream
import java.util.*

class LanguageManager(private val plugin: LightWeightRW) {

    private val languages = mutableMapOf<String, FileConfiguration>()
    private val playerLanguages = mutableMapOf<UUID, String>()

    fun loadLanguages() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        // 保存默认语言文件
        saveDefaultLanguageFiles()

        // 加载所有语言文件
        langFolder.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "yml") {
                val langCode = file.nameWithoutExtension
                val config = YamlConfiguration.loadConfiguration(file)
                languages[langCode] = config
                plugin.logger.info("Loaded language: $langCode")
            }
        }

        plugin.logger.info("Loaded ${languages.size} language(s)")
    }

    private fun saveDefaultLanguageFiles() {
        val langFiles = listOf("en_US.yml", "zh_CN.yml")

        langFiles.forEach { fileName ->
            val file = File(plugin.dataFolder, "lang/$fileName")
            if (!file.exists()) {
                val resource: InputStream? = plugin.getResource("lang/$fileName")
                if (resource != null) {
                    file.parentFile.mkdirs()
                    file.writeBytes(resource.readBytes())
                    resource.close()
                }
            }
        }
    }

    fun getMessage(key: String, player: Player? = null, vararg args: Any): String {
        val langCode = getPlayerLanguage(player)
        val config = languages[langCode] ?: languages[plugin.configManager.getDefaultLanguage()]
        ?: return "Missing message: $key"

        var message = config.getString(key) ?: return "Missing message: $key"

        // 替换占位符
        args.forEachIndexed { index, arg ->
            message = message.replace("{$index}", arg.toString())
        }

        // 替换命名占位符
        if (args.isNotEmpty() && args[0] is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val placeholders = args[0] as Map<String, Any>
            placeholders.forEach { (placeholder, value) ->
                message = message.replace("{$placeholder}", value.toString())
            }
        }

        return message.replace("&", "§")
    }

    fun getPlayerLanguage(player: Player?): String {
        if (player == null || !plugin.configManager.isPerPlayerLanguage()) {
            return plugin.configManager.getDefaultLanguage()
        }

        return playerLanguages[player.uniqueId] ?: plugin.configManager.getDefaultLanguage()
    }

    fun setPlayerLanguage(player: Player, langCode: String) {
        if (languages.containsKey(langCode)) {
            playerLanguages[player.uniqueId] = langCode
        }
    }

    fun getAvailableLanguages(): Set<String> = languages.keys

    fun isLanguageAvailable(langCode: String): Boolean = languages.containsKey(langCode)
}