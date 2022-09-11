package io.slama

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.slama.commands.AutoRoleCommand
import io.slama.commands.CallCommand
import io.slama.commands.ChanGenCommand
import io.slama.commands.KevalCommand
import io.slama.commands.PollCommand
import io.slama.commands.RockPaperScissorsCommand
import io.slama.commands.TableCommand
import io.slama.core.BotConfiguration
import io.slama.core.registerGlobalCommands
import io.slama.core.registerGuildCommands
import io.slama.events.Shusher
import io.slama.events.clearAutoRoles
import io.slama.events.loadAutoRoles
import io.slama.managers.MailManager
import io.slama.utils.TaskScheduler
import kotlinx.coroutines.Job
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

private val logger: Logger = LoggerFactory.getLogger("UGEBot")
private val token = Key("token", stringType)

class UGEBot(token: String) : ListenerAdapter() {

    private val jda = try {
        JDABuilder
            .createDefault(token)
            .setChunkingFilter(ChunkingFilter.ALL)
            .addEventListeners(this)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .build()
    } catch (e: LoginException) {
        logger.error("Invalid token.")
        exitProcess(1)
    }

    private var mailManager: MailManager? = null

    private var presenceJob: Job? = null

    init {
        while (true) {
            val command = readLine()?.split(" ") ?: listOf("default")
            when (command[0]) {
                "die" -> {
                    mailManager?.close()
                    jda.shutdown()
                    exitProcess(0)
                }
                "reload", "reset" -> {
                    logger.warn("Reload is not reliable, consider restarting the bot if you encounter issues")
                    mailManager?.close()
                    load()
                    logger.info("Reload complete!")
                }
                "mail-close" -> mailManager?.close()
                "mail-open" -> mailManager?.reOpen()
                "delete-commands" -> {
                    if (command.size < 2) continue
                    command.drop(1).forEach { name ->
                        deleteCommandByName(name)
                    }
                }
                "delete-global-commands" -> {
                    if (command.size < 2) continue
                    command.drop(1).forEach { name ->
                        deleteGlobalCommandByName(name)
                    }
                }
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        jda.addEventListener(
            CallCommand(),
            KevalCommand(),
            AutoRoleCommand(),
            PollCommand(),
            ChanGenCommand(),
            TableCommand(),
            RockPaperScissorsCommand(),
        )
        Shusher(jda)
        load()
    }

    private fun load() {
        BotConfiguration.resetConfig()
        val mailConfig = BotConfiguration.mail
        if (mailConfig != null) mailManager = MailManager(mailConfig, jda)
        clearAutoRoles()
        jda.registerGlobalCommands()
        jda.guilds.forEach {
            it.loadAutoRoles()
            it.registerGuildCommands()
        }
        logger.info("Registered commands")
        presenceJob?.cancel()
        presenceJob = TaskScheduler.repeat(10, TimeUnit.MINUTES) {
            val (message, type) = BotConfiguration.presence?.messages?.entries?.random()?.toPair()
                ?: ("nothing" to Activity.ActivityType.DEFAULT)
            jda.presence.setPresence(Activity.of(type, message), false)
            true
        }
    }

    private fun deleteCommandByName(name: String) {
        jda.guilds.forEach { guild ->
            guild.retrieveCommands().map { commands ->
                commands.firstOrNull { it.name == name }
            }.queue { command ->
                command?.let {
                    guild.deleteCommandById(it.idLong).queue {
                        logger.info("Deleted command '$name' in guild ${guild.id}")
                    }
                } ?: logger.info("Command '$name' not found in guild ${guild.id}")
            }
        }
    }

    private fun deleteGlobalCommandByName(name: String) {
        jda.retrieveCommands().map { commands ->
            commands.firstOrNull { it.name == name }
        }.queue { command ->
            command?.let {
                jda.deleteCommandById(it.idLong).queue {
                    logger.info("Deleted global command '$name' (may take up to 1h)")
                }
            } ?: logger.info("Command '$name' not found")
        }
    }
}

fun main() {
    BotConfiguration.resetConfig()
    with(ConfigurationProperties.fromFile(File("bot.properties"))) {
        UGEBot(this[token])
    }
}
