package cc.vastsea.lwrw.managers

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStream

class LanguageManager(private val plugin: LightWeightRW) {

    private val languages = mutableMapOf<String, FileConfiguration>()

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

    fun getMessage(key: String, placeholders: Map<String, Any?> = emptyMap()): String {
        val langCode = plugin.configManager.getDefaultLanguage()
        val config = languages[langCode] ?: return "Missing message: $key"

        var message = config.getString(key) ?: return "Missing message: $key"

        // 替换占位符
        placeholders.forEach { (k, v) ->
            message = message.replace("{$k}", v?.toString() ?: "")
        }

        return message.replace("&", "§")
    }

    fun sendMessage(receiver: CommandSender, key: String, placeholders: Map<String, Any?> = emptyMap()) {
        receiver.sendMessage(getMessage(key, placeholders))
    }

}