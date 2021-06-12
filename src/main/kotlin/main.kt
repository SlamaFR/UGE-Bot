import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import commands.AutoRoleCommand
import commands.CallCommand
import events.loadAutoRoles
import commands.KevalCommand
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.addCommands
import java.io.File
import kotlin.system.exitProcess

val logger: Logger = LoggerFactory.getLogger("UGEBot")

val token = Key("token", stringType)
val config = ConfigurationProperties.fromFile(File("bot.properties"))

fun main() {
    UGEBot(config[token])
}

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
        load()
        jda.addEventListener(CallCommand(), KevalCommand(), AutoRoleCommand())
        jda.guildCache.forEach {
            it.loadAutoRoles()
        }
    }

    private fun load() {
        logger.info("Registered commands")
    }
}
