package cc.vastsea.lwrw.tasks

import cc.vastsea.lwrw.LightWeightRW
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ResetTask(private val plugin: LightWeightRW) {

    private var resetTask: BukkitTask? = null
    private var warningTasks = mutableListOf<BukkitTask>()

    fun scheduleResetTask() {
        if (!plugin.configManager.isAutoResetEnabled()) {
            return
        }

        cancelResetTask()

        val resetTimeString = plugin.configManager.getResetTime()
        val resetTime = parseTime(resetTimeString)

        if (resetTime == null) {
            plugin.logger.warning("Invalid reset time format: $resetTimeString")
            return
        }

        val now = LocalDateTime.now()
        var nextReset = now.toLocalDate().atTime(resetTime)

        // 如果今天的重置时间已过，则安排到明天
        if (nextReset.isBefore(now) || nextReset.isEqual(now)) {
            nextReset = nextReset.plusDays(1)
        }

        val delayTicks = ChronoUnit.SECONDS.between(now, nextReset) * 20L

        plugin.logger.info("Next resource world reset scheduled for: ${nextReset.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")

        // 安排重置任务
        resetTask = object : BukkitRunnable() {
            override fun run() {
                performReset()
                // 安排下一次重置
                scheduleResetTask()
            }
        }.runTaskLater(plugin, delayTicks)

        // 安排警告任务
        scheduleWarningTasks(nextReset)
    }

    private fun scheduleWarningTasks(resetTime: LocalDateTime) {
        val warningTimes = plugin.configManager.getResetWarningTimes()
        val now = LocalDateTime.now()

        warningTimes.forEach { warningMinutes ->
            val warningTime = resetTime.minusMinutes(warningMinutes.toLong())

            if (warningTime.isAfter(now)) {
                val delayTicks = ChronoUnit.SECONDS.between(now, warningTime) * 20L

                val warningTask = object : BukkitRunnable() {
                    override fun run() {
                        sendResetWarning(warningMinutes)
                    }
                }.runTaskLater(plugin, delayTicks)

                warningTasks.add(warningTask)
            }
        }
    }

    private fun sendResetWarning(minutes: Int) {
        // 向所有在线玩家发送警告
        plugin.server.onlinePlayers.forEach { player ->
            val playerMessage = if (minutes >= 1) {
                plugin.languageManager.getMessage(
                    "reset-warning",
                    mapOf("time" to minutes)
                )
            } else {
                val seconds = minutes * 60
                plugin.languageManager.getMessage(
                    "reset-warning-seconds",
                    mapOf("time" to seconds)
                )
            }
            player.sendMessage(playerMessage)
        }
    }

    private fun performReset() {
        plugin.logger.info("Starting automatic resource world reset...")

        // 异步执行重置
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val success = plugin.worldManager.resetResourceWorld()

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (success) {
                    // 向所有在线玩家发送重置完成消息
                    plugin.server.onlinePlayers.forEach { player ->
                        val message = plugin.languageManager.getMessage("reset-complete")
                        player.sendMessage(message)
                    }

                    plugin.logger.info("Automatic resource world reset completed successfully!")
                } else {
                    plugin.logger.severe("Automatic resource world reset failed!")
                }
            })
        })
    }

    fun cancelResetTask() {
        resetTask?.cancel()
        resetTask = null

        warningTasks.forEach { it.cancel() }
        warningTasks.clear()
    }

    private fun parseTime(timeString: String): LocalTime? {
        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                LocalTime.of(hour, minute)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

}