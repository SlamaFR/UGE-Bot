package commands

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import utils.TaskScheduler
import utils.isTeacher
import utils.pluralize
import java.awt.Color
import java.time.Instant
import java.util.concurrent.TimeUnit

const val DEFAULT_POLL_TIMEOUT = 2L

class PollCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "poll") return
        if (event.guild == null) return
        if (!isTeacher(event.member!!)) return

        Poll(event, event.getOption("timeout")?.asLong ?: DEFAULT_POLL_TIMEOUT).send()
    }

}

class Poll(
    private val event: SlashCommandEvent,
    private val timeout: Long
) : ListenerAdapter() {

    private val uniqueId = event.user.id + System.currentTimeMillis().toString()
    private val answers = mutableMapOf<Int, MutableSet<Long>>()
    private val question = event.getOption("question")

    private var options = listOfNotNull(
        event.getOption("a")?.asString,
        event.getOption("b")?.asString,
        event.getOption("c")?.asString,
        event.getOption("d")?.asString,
    )

    init {
        event.jda.addEventListener(this)
        options = options.ifEmpty { mutableListOf("Oui", "Non") }
        options.forEachIndexed { i, _ -> answers[i] = mutableSetOf() }
    }

    fun send() {
        if (question != null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("Sondage demandé par ${event.member!!.effectiveName}")
                    .setDescription(question.asString)
                    .setFooter("Résultats du sondage dans $timeout minute".pluralize(timeout))
                    .setColor(Color(0x9b59b6))
                    .apply {
                        options.forEachIndexed { i, name ->
                            if (i % 2 != 0) addBlankField(true)
                            addField("Réponse ${'A' + i}", name, true)
                        }
                    }.build()
            ).addActionRows(ActionRow.of(
                options.mapIndexed { i, _ -> Button.secondary("$uniqueId.${i}", ('A' + i).toString()) }
            )).queue {
                TaskScheduler.later(timeout, TimeUnit.MINUTES) {
                    sendResults()
                }
            }
        }
    }

    private fun vote(option: Int, id: Long) {
        answers.forEach { it.value.remove(id) }
        answers[option]?.add(id)
    }

    private fun answerRate(option: Int): Double {
        val rate = answers[option]?.count()?.div(totalVoteCount().toDouble()) ?: 0.0
        return if (rate.isNaN()) 0.0 else rate
    }

    private fun totalVoteCount(): Int = answers.flatMap { it.value }.count()

    private fun sendResults() {
        event.jda.removeEventListener(this)
        if (question != null) {
            event.hook.editOriginalEmbeds(
                EmbedBuilder()
                    .setTitle("Sondage demandé par ${event.member!!.effectiveName}")
                    .setDescription(question.asString)
                    .setFooter("Sondage terminé (${totalVoteCount()} ${"votant".pluralize(totalVoteCount())})")
                    .setTimestamp(Instant.now())
                    .setColor(Color(0x9b59b6))
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
            event.hook.editOriginalComponents(ActionRow.of(
                options.mapIndexed { i, _ -> Button.secondary("$uniqueId.${i}", ('A' + i).toString()).asDisabled() }
            )).queue()
        }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (!event.componentId.startsWith(uniqueId)) return
        vote(event.componentId.split(".")[1].toInt(), event.user.idLong)
        event.reply(":white_check_mark: Votre vote a été pris en compte.").setEphemeral(true).queue()
    }
}
