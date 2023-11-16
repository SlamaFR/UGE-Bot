package io.slama.managers

import io.slama.commands.Call
import io.slama.commands.DEFAULT_CALL_TIMEOUT
import io.slama.core.BotConfiguration
import io.slama.core.ScheduledCallDTO
import io.slama.utils.TaskScheduler
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory

private val callsSchedulers = mutableMapOf<Long, CallScheduler>()
private val logger = LoggerFactory.getLogger("ScheduledCall")

class CallScheduler(
    private val guildId: Long,
    private val jda: JDA,
) {

    private val calls: Queue<ScheduledCallDTO> = LinkedBlockingQueue()
    private var nextCallJob: Job? = null

    fun load() {
        logger.info("${guildId}: Initializing call scheduler")

        calls.clear()
        nextCallJob?.cancel()

        val guildConfig = BotConfiguration.guilds[guildId] ?: run {
            logger.error("$guildId: No configuration found!")
            return
        }

        val now = Instant.now()
        guildConfig.scheduledCalls
            .filter { Instant.parse(it.executionTime).isAfter(now) }
            .sortedBy { Instant.parse(it.executionTime) }
            .forEach { calls.add(it) }

        scheduleNextCall()
    }

    private fun scheduleNextCall() {
        if (calls.isEmpty()) {
            logger.info("${guildId}: No more calls to schedule")
            return
        }

        val call = calls.poll()
        val executionTime = Instant.parse(call.executionTime)
        val guild = jda.getGuildById(guildId) ?: return
        val member = guild.getMemberById(call.recipientId) ?: return
        val channel = guild.getTextChannelById(call.channelId) ?: return
        val role = call.roleId?.let { guild.getRoleById(it) }

        val delay = executionTime.toEpochMilli() - System.currentTimeMillis()
        logger.info("${guildId}: Next call at $executionTime (in ${delay / 1000}s)")
        nextCallJob = TaskScheduler.later(delay, TimeUnit.MILLISECONDS) {
            scheduleNextCall()
            Call(jda, member, channel, call.timeout ?: DEFAULT_CALL_TIMEOUT, role)
        }
    }
}

fun Guild.initCallScheduler(jda: JDA) = callsSchedulers
    .getOrPut(this.idLong) { CallScheduler(this.idLong, jda) }
    .load()
