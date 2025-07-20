package cc.vastsea.lwrw.managers

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.random.Random

class WorldManager(private val plugin: LightWeightRW) {

    private val teleportCooldowns = mutableMapOf<UUID, Long>()

    fun getResourceWorld(): World? {
        val worldName = plugin.configManager.getResourceWorldName()
        return Bukkit.getWorld(worldName) ?: createResourceWorld()
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

    fun teleportToResourceWorld(player: Player): Boolean {
        // 检查冷却时间
        if (!checkCooldown(player)) {
            val remainingTime = getRemainingCooldown(player)
            val message = plugin.languageManager.getMessage(
                "teleport-cooldown",
                player,
                mapOf("time" to remainingTime)
            )
            player.sendMessage(message)
            return false
        }

        val world = getResourceWorld()
        if (world == null) {
            val message = plugin.languageManager.getMessage("world-not-found", player)
            player.sendMessage(message)
            return false
        }

        // 寻找安全的传送位置
        val safeLocation = findSafeLocation(world)
        if (safeLocation == null) {
            val message = plugin.languageManager.getMessage("teleport-unsafe", player)
            player.sendMessage(message)
            return false
        }

        // 执行传送
        player.teleport(safeLocation)
        setCooldown(player)

        val message = plugin.languageManager.getMessage("teleport-success", player)
        player.sendMessage(message)

        return true
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

    private fun checkCooldown(player: Player): Boolean {
        if (player.hasPermission("lwrw.admin.bypass")) {
            return true
        }

        val lastTeleport = teleportCooldowns[player.uniqueId] ?: 0L
        val cooldownTime = plugin.configManager.getTeleportCooldown() * 1000L

        return System.currentTimeMillis() - lastTeleport >= cooldownTime
    }

    private fun getRemainingCooldown(player: Player): Long {
        val lastTeleport = teleportCooldowns[player.uniqueId] ?: 0L
        val cooldownTime = plugin.configManager.getTeleportCooldown() * 1000L
        val remaining = cooldownTime - (System.currentTimeMillis() - lastTeleport)

        return (remaining / 1000L).coerceAtLeast(0L)
    }

    private fun setCooldown(player: Player) {
        if (!player.hasPermission("lwrw.admin.bypass")) {
            teleportCooldowns[player.uniqueId] = System.currentTimeMillis()
        }
    }

    fun resetResourceWorld(): Boolean {
        val worldName = plugin.configManager.getResourceWorldName()
        val world = Bukkit.getWorld(worldName)

        if (world != null) {
            // 踢出所有玩家
            if (plugin.configManager.shouldKickPlayersOnReset()) {
                world.players.forEach { player ->
                    val spawnWorld = Bukkit.getWorlds()
                        .firstOrNull { it.environment == World.Environment.NORMAL && it.name != worldName }
                    if (spawnWorld != null) {
                        player.teleport(spawnWorld.spawnLocation)
                        val message = plugin.languageManager.getMessage("reset-kicked", player)
                        player.sendMessage(message)
                    }
                }
            }

            // 卸载世界
            Bukkit.unloadWorld(world, false)

            // 删除世界文件
            val worldFolder = File(Bukkit.getWorldContainer(), worldName)
            if (worldFolder.exists()) {
                deleteDirectory(worldFolder)
            }
        }

        // 重新创建世界
        val newWorld = createResourceWorld()
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