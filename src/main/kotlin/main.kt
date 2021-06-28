import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import commands.AutoRoleCommand
import commands.CallCommand
import events.loadAutoRoles
import commands.KevalCommand
import commands.PollCommand
import core.clearGuildConfigs
import core.registerGlobalCommands
import core.registerGuildCommands
import events.clearAutoRoles
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
            when (readLine()) {
                "die" -> exitProcess(0)
                "reload" -> load()
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
            it.registerGuildCommands()
            it.loadAutoRoles()
        }
        logger.info("Registered commands")
    }
}

fun main() {
    UGEBot(config[token])
}
