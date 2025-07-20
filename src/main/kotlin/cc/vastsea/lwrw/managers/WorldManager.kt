package cc.vastsea.lwrw.managers

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.*
import java.io.File
import kotlin.random.Random

class WorldManager(private val plugin: LightWeightRW) {

    private fun getResourceWorld(): World? {
        val worldName = plugin.configManager.getResourceWorldName()
        val world = Bukkit.getWorld(worldName)
        if (world != null) return world
        // 保证在主线程创建世界
        return if (!Bukkit.isPrimaryThread()) {
            plugin.server.scheduler.callSyncMethod(plugin) {
                createResourceWorld()
            }.get()
        } else {
            createResourceWorld()
        }
    }

    private fun createResourceWorld(): World? {
        val worldName = plugin.configManager.getResourceWorldName()
        val seed = plugin.configManager.getWorldSeed()

        plugin.logger.info("Creating resource world: $worldName")

        val worldCreator = WorldCreator(worldName)
            .type(WorldType.NORMAL)
            .environment(World.Environment.NORMAL)
            .generateStructures(true)

        if (seed != 0L) {
            worldCreator.seed(seed)
        }

        val world = worldCreator.createWorld()

        if (world != null) {
            // 设置世界规则
            world.setGameRule(GameRule.KEEP_INVENTORY, false)
            world.setGameRule(GameRule.DO_MOB_SPAWNING, true)
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true)
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, true)
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

            plugin.logger.info("Resource world created successfully!")
        } else {
            plugin.logger.severe("Failed to create resource world!")
        }

        return world
    }

    /**
     * 查找安全位置并返回，主线程调用此方法进行传送
     */
    fun findSafeLocationForTeleport(): Pair<Location?, String?> {
        val world = getResourceWorld()
        if (world == null) {
            val message = plugin.languageManager.getMessage("world-not-found")
            return Pair(null, message)
        }
        val safeLocation = findSafeLocation(world)
        if (safeLocation == null) {
            val message = plugin.languageManager.getMessage("teleport-unsafe")
            return Pair(null, message)
        }
        return Pair(safeLocation, null)
    }

    private fun findSafeLocation(world: World): Location? {
        val radius = plugin.configManager.getTeleportRadius()
        val minY = plugin.configManager.getMinY()
        val maxY = plugin.configManager.getMaxY()

        repeat(50) { // 最多尝试50次
            val x = Random.nextInt(-radius, radius + 1)
            val z = Random.nextInt(-radius, radius + 1)

            // 获取最高的固体方块
            val highestY = world.getHighestBlockYAt(x, z)
            val y = highestY.coerceIn(minY, maxY)

            val location = Location(world, x.toDouble() + 0.5, y.toDouble() + 1, z.toDouble() + 0.5)

            // 检查位置是否安全
            if (isSafeLocation(location)) {
                return location
            }
        }

        return null
    }

    private fun isSafeLocation(location: Location): Boolean {
        val world = location.world ?: return false
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        // 检查脚下是否有固体方块
        val groundBlock = world.getBlockAt(x, y - 1, z)
        if (!groundBlock.type.isSolid) {
            return false
        }

        // 检查头部和身体位置是否为空气
        val feetBlock = world.getBlockAt(x, y, z)
        val headBlock = world.getBlockAt(x, y + 1, z)

        return feetBlock.type.isAir && headBlock.type.isAir
    }

    fun resetResourceWorld(): Boolean {
        val worldName = plugin.configManager.getResourceWorldName()
        val world = Bukkit.getWorld(worldName)

        if (world != null) {
            // 踢出所有玩家（主线程）
            if (plugin.configManager.shouldKickPlayersOnReset()) {
                val kickPlayers: (World) -> Unit = { w ->
                    w.players.forEach { player ->
                        val spawnWorld = Bukkit.getWorlds()
                            .firstOrNull { it.environment == World.Environment.NORMAL && it.name != worldName }
                        if (spawnWorld != null) {
                            player.teleport(spawnWorld.spawnLocation)
                            val message = plugin.languageManager.getMessage("reset-kicked")
                            player.sendMessage(message)
                        }
                    }
                }
                if (!Bukkit.isPrimaryThread()) {
                    plugin.server.scheduler.callSyncMethod(plugin) {
                        kickPlayers(world)
                    }.get()
                } else {
                    kickPlayers(world)
                }
            }

            // 卸载世界，必须在主线程
            if (!Bukkit.isPrimaryThread()) {
                plugin.server.scheduler.callSyncMethod(plugin) {
                    Bukkit.unloadWorld(world, false)
                }.get()
            } else {
                Bukkit.unloadWorld(world, false)
            }

            // 删除世界文件
            val worldFolder = File(Bukkit.getWorldContainer(), worldName)
            if (worldFolder.exists()) {
                deleteDirectory(worldFolder)
            }
        }

        // 清除所有玩家的传送门提示状态
        plugin.portalListener.clearAllNotifications()

        // 重新创建世界，必须在主线程
        val newWorld = if (!Bukkit.isPrimaryThread()) {
            plugin.server.scheduler.callSyncMethod(plugin) {
                createResourceWorld()
            }.get()
        } else {
            createResourceWorld()
        }
        return newWorld != null
    }

    private fun deleteDirectory(directory: File): Boolean {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                deleteDirectory(file)
            }
        }
        return directory.delete()
    }

    fun isResourceWorld(world: World): Boolean {
        return world.name == plugin.configManager.getResourceWorldName()
    }
}