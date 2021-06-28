import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import commands.AutoRoleCommand
import commands.CallCommand
import commands.KevalCommand
import commands.PollCommand
import core.clearGuildConfigs
import core.registerGlobalCommands
import core.registerGuildCommands
import events.clearAutoRoles
import events.loadAutoRoles
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

private val logger: Logger = LoggerFactory.getLogger("UGEBot")
private val token = Key("token", stringType)
private val config = ConfigurationProperties.fromFile(File("bot.properties"))

class UGEBot(token: String) : ListenerAdapter() {

    private val jda = JDABuilder
        .createDefault(token)
        .setChunkingFilter(ChunkingFilter.ALL)
        .addEventListeners(this)
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .build()

    init {
        while (true) {
            val command = readLine()?.split(" ") ?: listOf("default")
            when (command[0]) {
                "die" -> exitProcess(0)
                "reload" -> load()
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
        jda.addEventListener(CallCommand(), KevalCommand(), AutoRoleCommand(), PollCommand())
        load()
    }

    private fun load() {
        clearAutoRoles()
        clearGuildConfigs()
        jda.registerGlobalCommands()
        jda.guilds.forEach {
            it.loadAutoRoles()
            it.registerGuildCommands()
        }
        logger.info("Registered commands")
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
    UGEBot(config[token])
}
