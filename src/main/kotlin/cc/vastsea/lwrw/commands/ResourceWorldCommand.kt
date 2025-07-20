package cc.vastsea.lwrw.commands

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ResourceWorldCommand(private val plugin: LightWeightRW) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "help" -> showHelp(sender)
            "tp", "teleport" -> handleTeleport(sender)
            "reset" -> handleReset(sender)
            "reload" -> handleReload(sender)
            "lang", "language" -> handleLanguage(sender, args)
            else -> {
                val message = plugin.languageManager.getMessage("invalid-args", sender as? Player)
                sender.sendMessage(message)
            }
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {
        val player = sender as? Player

        sender.sendMessage(plugin.languageManager.getMessage("help-header", player))
        sender.sendMessage(plugin.languageManager.getMessage("help-tp", player))

        if (sender.hasPermission("lwrw.admin")) {
            sender.sendMessage(plugin.languageManager.getMessage("help-reset", player))
            sender.sendMessage(plugin.languageManager.getMessage("help-reload", player))
        }

        if (plugin.configManager.isPerPlayerLanguage()) {
            sender.sendMessage(plugin.languageManager.getMessage("help-lang", player))
        }

        sender.sendMessage(plugin.languageManager.getMessage("help-help", player))
        sender.sendMessage(plugin.languageManager.getMessage("help-footer", player))
    }

    private fun handleTeleport(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("player-only"))
            return
        }

        if (!sender.hasPermission("lwrw.use.tp")) {
            sender.sendMessage(plugin.languageManager.getMessage("no-permission", sender))
            return
        }

        sender.sendMessage(plugin.languageManager.getMessage("teleport-searching", sender))

        // 异步执行传送以避免阻塞主线程
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val success = plugin.worldManager.teleportToResourceWorld(sender)
            if (!success) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage(plugin.languageManager.getMessage("teleport-failed", sender))
                })
            }
        })
    }

    private fun handleReset(sender: CommandSender) {
        if (!sender.hasPermission("lwrw.admin.reset")) {
            val message = plugin.languageManager.getMessage("no-permission", sender as? Player)
            sender.sendMessage(message)
            return
        }

        val player = sender as? Player
        sender.sendMessage(plugin.languageManager.getMessage("reset-started", player))

        // 异步执行重置以避免阻塞主线程
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val success = plugin.worldManager.resetResourceWorld()

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (success) {
                    sender.sendMessage(plugin.languageManager.getMessage("reset-manual-success", player))

                    // 广播重置消息
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        onlinePlayer.sendMessage(plugin.languageManager.getMessage("reset-complete", onlinePlayer))
                    }
                } else {
                    sender.sendMessage(plugin.languageManager.getMessage("reset-manual-failed", player))
                }
            })
        })
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("lwrw.admin.reload")) {
            val message = plugin.languageManager.getMessage("no-permission", sender as? Player)
            sender.sendMessage(message)
            return
        }

        try {
            plugin.reload()
            val message = plugin.languageManager.getMessage("config-reloaded", sender as? Player)
            sender.sendMessage(message)
        } catch (e: Exception) {
            val message = plugin.languageManager.getMessage("config-reload-failed", sender as? Player)
            sender.sendMessage(message)
            plugin.logger.severe("Failed to reload configuration: ${e.message}")
        }
    }

    private fun handleLanguage(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("player-only"))
            return
        }

        if (!plugin.configManager.isPerPlayerLanguage()) {
            sender.sendMessage(plugin.languageManager.getMessage("invalid-args", sender))
            return
        }

        if (args.size < 2) {
            val availableLanguages = plugin.languageManager.getAvailableLanguages().joinToString(", ")
            val message = plugin.languageManager.getMessage(
                "language-invalid",
                sender,
                mapOf("languages" to availableLanguages)
            )
            sender.sendMessage(message)
            return
        }

        val langCode = args[1]

        if (!plugin.languageManager.isLanguageAvailable(langCode)) {
            val availableLanguages = plugin.languageManager.getAvailableLanguages().joinToString(", ")
            val message = plugin.languageManager.getMessage(
                "language-invalid",
                sender,
                mapOf("languages" to availableLanguages)
            )
            sender.sendMessage(message)
            return
        }

        plugin.languageManager.setPlayerLanguage(sender, langCode)
        val message = plugin.languageManager.getMessage(
            "language-changed",
            sender,
            mapOf("language" to langCode)
        )
        sender.sendMessage(message)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subcommands = mutableListOf("help", "tp")

            if (sender.hasPermission("lwrw.admin")) {
                subcommands.addAll(listOf("reset", "reload"))
            }

            if (plugin.configManager.isPerPlayerLanguage() && sender is Player) {
                subcommands.add("lang")
            }

            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].lowercase() in listOf("lang", "language")) {
            return plugin.languageManager.getAvailableLanguages().filter {
                it.startsWith(args[1], ignoreCase = true)
            }
        }

        return emptyList()
    }
}