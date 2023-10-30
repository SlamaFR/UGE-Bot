package io.slama.managers

import io.slama.commands.Call
import io.slama.commands.DEFAULT_CALL_TIMEOUT
import io.slama.utils.TaskScheduler
import java.io.File
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

private const val CONFIG_ROOT = "config/"
private const val SCHEDULED_CALLS = "${CONFIG_ROOT}scheduledCalls.json"

private val logger = LoggerFactory.getLogger("ScheduledCall")

object ScheduledCallsManager {

    private val calls: Queue<CallRepresentation> = LinkedBlockingQueue()
    private lateinit var jdaInstance: JDA

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadConfig() {
        with(File(SCHEDULED_CALLS)) {
            if (exists()) {
                val now = Instant.now()
                Json.decodeFromStream<List<CallRepresentation>>(inputStream())
                    .filter { Instant.parse(it.executionTime).isAfter(now) }
                    //.sortedBy { Instant.parse(it.executionTime) }
                    .forEach { calls.add(it) }
            }
        }

        println(calls)
    }

    fun start(jda: JDA) {
        jdaInstance = jda
        loadConfig()
        scheduleNextCall(jda)
    }

    private fun scheduleNextCall(jda: JDA) {
        if (calls.isEmpty()) return

        val call = calls.poll()
        val executionTime = Instant.parse(call.executionTime)
        val guild = jda.getGuildById(call.guildId) ?: return
        val member = guild.getMemberById(call.recipientId) ?: return
        val channel = guild.getTextChannelById(call.channelId) ?: return
        val role = call.roleId?.let { guild.getRoleById(it) }

        val delay = executionTime.toEpochMilli() - System.currentTimeMillis()
        TaskScheduler.later(delay, TimeUnit.MILLISECONDS) {
            scheduleNextCall(jda)
            logger.info("Scheduled call for ${member.effectiveName} in $channel at $executionTime (in ${delay}ms)")
            Call(jda, member, channel, call.timeout ?: DEFAULT_CALL_TIMEOUT, role)
        }
    }

}

@Serializable
data class CallRepresentation(
    val guildId: String,
    val channelId: String,
    val recipientId: String,
    val roleId: String? = null,
    val timeout: Long? = 5,
    val executionTime: String,
)
