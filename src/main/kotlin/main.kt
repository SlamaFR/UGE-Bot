import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import commands.CallCommand
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        while(true) {
            when (readLine()) {
                "die" -> exitProcess(0)
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        registerCommands()
        jda.addEventListener(CallCommand())
    }

    /*
     * Hacky solution for now, will change.
     */
    private fun registerCommands() {
        val guild = jda.getGuildById("393141696793149450") ?: return

        guild {
            command(name = "call", "Lancer un appel dans le salon courant.") {
                option(OptionType.INTEGER, name = "timeout", "Temps imparti pour répondre à l'appel en minutes.")
                //option(OptionType.ROLE, name = "role", "Groupe d'étudiants visé") not working yet.
            }
        }
    }
}
