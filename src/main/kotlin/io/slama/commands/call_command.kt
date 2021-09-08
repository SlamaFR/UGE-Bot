package io.slama.commands

import io.slama.utils.EmbedColors
import io.slama.utils.TaskScheduler
import io.slama.utils.isTeacher
import io.slama.utils.pluralize
import io.slama.utils.replySuccess
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val DEFAULT_TIMEOUT = 5L

class CallCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "call") return
        if (event.guild == null) return
        if (!event.member!!.isTeacher()) return

        Call(event, event.getOption("timeout")?.asLong ?: DEFAULT_TIMEOUT)
    }

}

private class Call(
    private val event: SlashCommandEvent,
    private val timeout: Long = DEFAULT_TIMEOUT
) : ListenerAdapter() {

    private val students = mutableSetOf<String>()
    private val uniqueId = "${event.user.id}${System.currentTimeMillis()}"
    private val embedTitle = "Appel demandé par ${event.member?.effectiveName ?: "un petit chachottier"}"

    init {
        event.jda.addEventListener(this)

        event.replyEmbeds(EmbedBuilder()
            .setTitle(embedTitle)
            .setDescription("Vous avez $timeout ${"minute".pluralize(timeout.toInt())} pour répondre à l'appel.")
            .setColor(EmbedColors.GREEN)
            .build())
            .addActionRow(Button.success("$uniqueId.respond", "Répondre à l'appel"))
            .queue {
                TaskScheduler.later(timeout, TimeUnit.MINUTES, ::sendResult)
            }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.componentId != "$uniqueId.respond") return
        event.member?.run {
            if (effectiveName !in students) {
                event.replySuccess(":white_check_mark: Votre présence a été enregistrée.").setEphemeral(true).queue()
                students.add(effectiveName)
            }
        }
    }

    private fun sendResult() {
        event.jda.removeEventListener(this)

        val calendar = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")
        val hdf = SimpleDateFormat("dd/MM/yyyy à HH:mm")
        val fileName = "${event.member?.effectiveName ?: "anonymous"}_#${event.textChannel.name}_${df.format(calendar.time)}.txt"

        File(fileName).apply {
            if (!createNewFile()) {
                onResultFailed()
                return
            }
            calendar.add(Calendar.MINUTE, (-timeout).toInt())

            bufferedWriter().use { out ->
                out.write(
                    """
                    |Appel effectué le ${hdf.format(calendar.time)} par ${event.member?.effectiveName ?: "un certain A. N. Onym"} dans le salon #${event.textChannel.name}
                    |
                    |${students.size} ${"personne".pluralize(students.size)} ${"présente".pluralize(students.size)} :
                    |${students.joinToString("\n") { " - $it" }}
                    |
                """.trimMargin()
                )
            }

            event.member?.user?.openPrivateChannel()?.queue {
                it.sendFile(this).queue({ onResultSuccess() }, { onResultFailed() })
            } ?: onResultFailed()
        }
    }

    private fun onResultSuccess() {
        event.hook.editOriginalEmbeds(EmbedBuilder()
            .setTitle(embedTitle)
            .setDescription("L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${"présente".pluralize(students.size)}.")
            .setColor(EmbedColors.ORANGE)
            .build())
            .queue()
        event.hook.editOriginalComponents(ActionRow.of(
            Button.danger("0", "Appel terminé").withDisabled(true)))
            .queue()
    }

    private fun onResultFailed() {
        event.hook.editOriginalEmbeds(EmbedBuilder()
            .setTitle("Appel demandé par ${event.member?.effectiveName ?: "un certain A. N. Onym"}")
            .setDescription(
                """
                |L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${
                    "présente".pluralize(
                        students.size
                    )
                }.
                |
                |**Une erreur est survenue lors de l'envoi du fichier !**
            """.trimMargin()
            )
            .setColor(EmbedColors.RED)
            .build())
            .queue()
        event.hook.editOriginalComponents(ActionRow.of(
            Button.danger("0", "Appel terminé").withDisabled(true)))
            .queue()
    }

}
