package cc.vastsea.lwrw.commands

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

class ResourceWorldCommand(private val plugin: LightWeightRW) : CommandExecutor, TabCompleter {
    // 记录玩家上次/rw tp前的位置
    private val lastLocations = mutableMapOf<UUID, org.bukkit.Location>()

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
            "back" -> handleBack(sender)
            else -> {
                val message = plugin.languageManager.getMessage("invalid-args", emptyMap())
                sender.sendMessage(message)
            }
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {

        sender.sendMessage(plugin.languageManager.getMessage("help-header", emptyMap()))
        sender.sendMessage(plugin.languageManager.getMessage("help-tp", emptyMap()))

        if (sender.hasPermission("lwrw.admin")) {
            sender.sendMessage(plugin.languageManager.getMessage("help-reset", emptyMap()))
            sender.sendMessage(plugin.languageManager.getMessage("help-reload", emptyMap()))
        }

        sender.sendMessage(plugin.languageManager.getMessage("help-help", emptyMap()))
        sender.sendMessage(plugin.languageManager.getMessage("help-footer", emptyMap()))
    }

    private fun handleTeleport(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("player-only"))
            return
        }

        if (!sender.hasPermission("lwrw.use.tp")) {
            sender.sendMessage(plugin.languageManager.getMessage("no-permission", emptyMap()))
            return
        }

        // 检查玩家是否已经在资源世界中
        if (plugin.worldManager.isResourceWorld(sender.world)) {
            // 如果已经在资源世界，直接进行随机传送
            sender.sendMessage(plugin.languageManager.getMessage("teleport-searching", emptyMap()))

            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                val (safeLocation, failMessage) = plugin.worldManager.findSafeLocationForTeleport()
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (safeLocation != null) {
                        sender.teleport(safeLocation)
                        sender.sendMessage(plugin.languageManager.getMessage("teleport-success", emptyMap()))
                    } else {
                        sender.sendMessage(failMessage ?: plugin.languageManager.getMessage("teleport-failed", emptyMap()))
                    }
                })
            })
        } else {
            // 如果不在资源世界，传送到资源世界
            sender.sendMessage(plugin.languageManager.getMessage("teleport-searching", emptyMap()))

            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                val (safeLocation, failMessage) = plugin.worldManager.findSafeLocationForTeleport()
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (safeLocation != null) {
                        // 记录玩家传送前的位置（只在从其他世界传送到资源世界时记录）
                        lastLocations[sender.uniqueId] = sender.location.clone()
                        sender.teleport(safeLocation)
                        sender.sendMessage(plugin.languageManager.getMessage("teleport-success", emptyMap()))
                    } else {
                        sender.sendMessage(failMessage ?: plugin.languageManager.getMessage("teleport-failed", emptyMap()))
                    }
                })
            })
        }
    }

    private fun handleReset(sender: CommandSender) {
        if (!sender.hasPermission("lwrw.admin.reset")) {
            val message = plugin.languageManager.getMessage("no-permission", emptyMap())
            sender.sendMessage(message)
            return
        }

        sender.sendMessage(plugin.languageManager.getMessage("reset-started", emptyMap()))

        // 异步执行重置以避免阻塞主线程
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val success = plugin.worldManager.resetResourceWorld()

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (success) {
                    sender.sendMessage(plugin.languageManager.getMessage("reset-manual-success", emptyMap()))

                    // 广播重置消息
                    plugin.server.onlinePlayers.forEach { onlinePlayer ->
                        onlinePlayer.sendMessage(plugin.languageManager.getMessage("reset-complete", emptyMap()))
                    }
                } else {
                    sender.sendMessage(plugin.languageManager.getMessage("reset-manual-failed", emptyMap()))
                }
            })
        })
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("lwrw.admin.reload")) {
            val message = plugin.languageManager.getMessage("no-permission", emptyMap())
            sender.sendMessage(message)
            return
        }

        try {
            plugin.reload()
            val message = plugin.languageManager.getMessage("config-reloaded", emptyMap())
            sender.sendMessage(message)
        } catch (e: Exception) {
            val message = plugin.languageManager.getMessage("config-reload-failed", emptyMap())
            sender.sendMessage(message)
            plugin.logger.severe("Failed to reload configuration: ${e.message}")
        }
    }

    private fun handleBack(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(plugin.languageManager.getMessage("player-only"))
            return
        }
        if (!sender.hasPermission("lwrw.use.back")) {
            sender.sendMessage(plugin.languageManager.getMessage("no-permission", emptyMap()))
            return
        }
        val resourceWorld = plugin.server.getWorld(plugin.configManager.getResourceWorldName())
        if (sender.world != resourceWorld) {
            sender.sendMessage(plugin.languageManager.getMessage("back-only-in-resource-world", emptyMap()))
            return
        }
        val lastLoc = lastLocations[sender.uniqueId]
        if (lastLoc != null) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.teleport(lastLoc)
                sender.sendMessage(plugin.languageManager.getMessage("teleport-back-success", emptyMap()))
            })
            lastLocations.remove(sender.uniqueId)
        } else {
            sender.sendMessage(plugin.languageManager.getMessage("back-no-location", emptyMap()))
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subcommands = mutableListOf("help", "tp", "back")

            if (sender.hasPermission("lwrw.admin")) {
                subcommands.addAll(listOf("reset", "reload"))
            }

            return subcommands.filter { it.startsWith(args[0].lowercase()) }
        }


        return emptyList()
    }
}