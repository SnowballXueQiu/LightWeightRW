package cc.vastsea.lwrw

import cc.vastsea.lwrw.commands.ResourceWorldCommand
import cc.vastsea.lwrw.config.ConfigManager
import cc.vastsea.lwrw.listeners.PortalListener
import cc.vastsea.lwrw.managers.LanguageManager
import cc.vastsea.lwrw.managers.WorldManager
import cc.vastsea.lwrw.tasks.ResetTask
import org.bukkit.plugin.java.JavaPlugin

class LightWeightRW : JavaPlugin() {

    companion object {
        lateinit var instance: LightWeightRW
            private set
    }

    lateinit var configManager: ConfigManager
    lateinit var languageManager: LanguageManager
    lateinit var worldManager: WorldManager
    lateinit var resetTask: ResetTask

    override fun onEnable() {
        instance = this

        logger.info("LightWeightRW is starting...")

        // 初始化配置管理器
        configManager = ConfigManager(this)
        configManager.loadConfig()

        // 初始化语言管理器
        languageManager = LanguageManager(this)
        languageManager.loadLanguages()

        // 初始化世界管理器
        worldManager = WorldManager(this)

        // 注册命令
        getCommand("rw")?.setExecutor(ResourceWorldCommand(this))

        // 注册监听器
        server.pluginManager.registerEvents(PortalListener(this), this)

        // 初始化重置任务
        resetTask = ResetTask(this)
        resetTask.scheduleResetTask()

        logger.info("LightWeightRW has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("LightWeightRW is shutting down...")

        // 取消所有任务
        if (::resetTask.isInitialized) {
            resetTask.cancelResetTask()
        }

        logger.info("LightWeightRW has been disabled successfully!")
    }

    fun reload() {
        configManager.loadConfig()
        languageManager.loadLanguages()
        resetTask.cancelResetTask()
        resetTask.scheduleResetTask()
    }
}