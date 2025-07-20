package cc.vastsea.lwrw.listeners

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.PortalCreateEvent

class PortalListener(private val plugin: LightWeightRW) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerPortal(event: PlayerPortalEvent) {
        val player = event.player
        val fromWorld = event.from.world

        // 检查是否在资源世界中
        if (fromWorld != null && plugin.worldManager.isResourceWorld(fromWorld)) {
            when (event.cause) {
                PlayerTeleportEvent.TeleportCause.NETHER_PORTAL -> {
                    if (plugin.configManager.isNetherDisabled()) {
                        event.isCancelled = true
                        val message = plugin.languageManager.getMessage("portal-nether-disabled", emptyMap())
                        player.sendMessage(message)
                    }
                }

                PlayerTeleportEvent.TeleportCause.END_PORTAL -> {
                    if (plugin.configManager.isEndDisabled()) {
                        event.isCancelled = true
                        val message = plugin.languageManager.getMessage("portal-end-disabled", emptyMap())
                        player.sendMessage(message)
                    }
                }

                else -> {
                    // 其他传送原因不处理
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPortalCreate(event: PortalCreateEvent) {
        val world = event.world

        // 检查是否在资源世界中
        if (plugin.worldManager.isResourceWorld(world)) {
            when (event.reason) {
                PortalCreateEvent.CreateReason.FIRE -> {
                    // 检查是否为地狱传送门
                    if (plugin.configManager.isNetherDisabled() && isNetherPortal(event)) {
                        event.isCancelled = true
                    }
                }

                PortalCreateEvent.CreateReason.END_PLATFORM -> {
                    // 末地传送门创建
                    if (plugin.configManager.isEndDisabled()) {
                        event.isCancelled = true
                    }
                }

                else -> {
                    // 其他创建原因不处理
                }
            }
        }
    }

    private fun isNetherPortal(event: PortalCreateEvent): Boolean {
        // 检查传送门方块是否包含地狱传送门材料
        return event.blocks.any { blockState ->
            blockState.type == Material.NETHER_PORTAL
        }
    }
}