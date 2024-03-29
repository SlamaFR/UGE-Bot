package io.slama.commands

import io.slama.core.BotConfiguration
import io.slama.core.ConfigFolders
import io.slama.utils.EmbedColors
import io.slama.utils.TaskScheduler
import io.slama.utils.pluralize
import io.slama.utils.replySuccess
import io.slama.utils.sendWarning
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

const val DEFAULT_POLL_TIMEOUT = 2L
const val DEFAULT_POLL_LOG = false

private val logger = LoggerFactory.getLogger("PollCommand")

class PollCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "poll") return
        if (event.guild == null) return

        Poll(
            event,
            event.getOption("timeout")?.asLong ?: DEFAULT_POLL_TIMEOUT,
            event.getOption("log")?.asBoolean ?: DEFAULT_POLL_LOG,
        ).send()
    }
}

class Poll(
    private val event: SlashCommandInteractionEvent,
    private val timeout: Long,
    private val toBeLogged: Boolean,
) : ListenerAdapter() {

    private val uniqueId = event.user.id + System.currentTimeMillis().toString()
    private val answers = mutableMapOf<Int, MutableSet<Long>>()
    private val effectiveNames = mutableMapOf<Long, String>()
    private val question = event.getOption("question")

    private var options = listOfNotNull(
        event.getOption("a")?.asString,
        event.getOption("b")?.asString,
        event.getOption("c")?.asString,
        event.getOption("d")?.asString,
    )

    private val totalVoteCount: Int
        get() = answers.flatMap { it.value }.count()

    private lateinit var responseId: String

    init {
        event.jda.addEventListener(this)
        options = options.ifEmpty { mutableListOf("Oui", "Non") }
        options.forEachIndexed { i, _ -> answers[i] = mutableSetOf() }
    }

    fun send() {
        if (question != null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("Sondage demandé par ${event.member?.effectiveName ?: "un certain A. N. Onym"}")
                    .setDescription(question.asString)
                    .setFooter("Résultats du sondage dans $timeout minute".pluralize(timeout))
                    .setColor(EmbedColors.VIOLET)
                    .apply {
                        options.forEachIndexed { i, name ->
                            if (i % 2 != 0) addBlankField(true)
                            addField("Réponse ${'A' + i}", name, true)
                        }
                    }.build()
            ).addActionRow(
                List(options.size) { i -> Button.secondary("poll.$uniqueId.$i", ('A' + i).toString()) }
            ).queue {
                it.retrieveOriginal().queue { message -> responseId = message.id }
                TaskScheduler.later(timeout, TimeUnit.MINUTES, ::sendResults)
            }
        }
    }

    private fun vote(option: Int, id: Long, effectiveName: String) {
        answers.forEach { it.value.remove(id) }
        answers[option]?.add(id)
        effectiveNames[id] = effectiveName
    }

    private fun answerRate(option: Int): Double {
        val rate = answers[option]?.count()?.div(totalVoteCount.toDouble()) ?: 0.0
        return if (rate.isNaN()) 0.0 else rate
    }

    private fun sendResults() {
        event.jda.removeEventListener(this)
        if (question != null) {
            event.channel.editMessageEmbedsById(
                responseId,
                EmbedBuilder()
                    .setTitle("Sondage demandé par ${event.member?.effectiveName ?: "un certain A. N. Onym"}")
                    .setDescription(question.asString)
                    .setFooter("Sondage terminé ($totalVoteCount ${"votant".pluralize(totalVoteCount)})")
                    .setTimestamp(Instant.now())
                    .setColor(EmbedColors.PURPLE)
                    .apply {
                        options.forEachIndexed { i, name ->
                            if (i % 2 != 0) addBlankField(true)
                            addField(
                                "Réponse ${'A' + i}",
                                "**(${"%.2f".format(answerRate(i) * 100)}%)** $name",
                                true
                            )
                        }
                    }.build()
            ).queue()
            event.channel.editMessageComponentsById(
                responseId,
                ActionRow.of(
                    List(options.size) { i -> Button.secondary("$uniqueId.$i", ('A' + i).toString()).asDisabled() }
                )
            ).queue()
        }

        if (toBeLogged) sendLog()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("poll.$uniqueId")) return
        vote(event.componentId.split(".")[2].toInt(), event.user.idLong, event.member?.effectiveName ?: "")
        event.replySuccess("Votre vote a été pris en compte.").setEphemeral(true).queue()
    }

    private fun onFailSendLog() {
        event.channel
            .sendWarning("Une erreur est survenue lors de l'envoi du fichier du sondage !")
            .queue()
    }

    private fun sendLog() {
        val calendar = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")
        val hdf = SimpleDateFormat("dd/MM/yyyy à HH:mm")
        val fileName =
            "poll_${event.member?.effectiveName ?: "anonymous"}_#${event.messageChannel.name}_${df.format(calendar.time)}.txt"

        with(File(ConfigFolders.POLLS_DATA_ROOT)) {
            if (!exists() || !isDirectory)
                BotConfiguration.resetConfig()
        }

        File(ConfigFolders.POLLS_DATA_ROOT, fileName).apply {
            if (!createNewFile()) {
                logger.error("Couldn't created poll file '$fileName'")
                onFailSendLog()
                return
            }
            calendar.add(Calendar.MINUTE, (-timeout).toInt())

            bufferedWriter().use { out ->
                out.write(
                    """
                    |Sondage effectué le ${hdf.format(calendar.time)} par ${event.member?.effectiveName ?: "anonymous"} dans le salon #${event.messageChannel.name}
                    |
                    |$totalVoteCount ${"personne".pluralize(totalVoteCount)} ${if (totalVoteCount > 1) "ont" else "à"} voté :
                    |Question : ${question?.asString ?: "Pas de question"}
                    |
                """.trimMargin()
                )

                options.forEachIndexed { i, name ->
                    val rate = "%.2f".format(answerRate(i) * 100)
                    val count = answers[i]?.count() ?: 0
                    out.write("${'A' + i} : $name -> $rate% ($count ${"vote".pluralize(count)})\n")
                    answers[i]?.forEach { id ->
                        out.write(" - ${effectiveNames[id]}\n")
                    }
                    out.write("\n")
                }
            }

            event.member?.user?.openPrivateChannel()?.queue {
                it.sendFiles(FileUpload.fromData(this)).queue({}, { onFailSendLog() })
            } ?: onFailSendLog()
        }
    }
}
