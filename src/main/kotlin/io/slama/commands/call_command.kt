package io.slama.commands

import io.slama.utils.TaskScheduler
import io.slama.utils.isTeacher
import io.slama.utils.pluralize
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import java.awt.Color
import java.io.File
import java.io.IOException
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

class Call(
    private val event: SlashCommandEvent,
    private val timeout: Long = DEFAULT_TIMEOUT
) : ListenerAdapter() {

    private val students = mutableSetOf<String>()
    private val uniqueId = event.user.id + System.currentTimeMillis().toString()

    init {
        event.jda.addEventListener(this)

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("Appel demandé par ${event.member!!.effectiveName}")
                .setDescription("Vous avez $timeout ${"minute".pluralize(timeout.toInt())} pour répondre à l'appel.")
                .setColor(Color(0x2ecc71))
                .build()
        ).addActionRow(Button.success("$uniqueId.respond", "Répondre à l'appel"))
            .queue {
                TaskScheduler.later(timeout, TimeUnit.MINUTES) {
                    sendResult()
                }
            }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.componentId != "$uniqueId.respond") return
        event.member?.let {
            if (it.effectiveName !in students)
                event.reply(":white_check_mark: Votre présence a été enregistrée.").setEphemeral(true).queue()
            students.add(it.effectiveName)
        }
    }

    private fun sendResult() {
        event.jda.removeEventListener(this)
        try {
            val calendar = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")
            val hdf = SimpleDateFormat("dd/MM/yyyy à HH:mm")
            val fileName = "${event.member?.effectiveName ?: "anonymous"}_#${event.textChannel.name}_${df.format(calendar.time)}.txt"

            File(fileName).apply {
                if (!createNewFile()) throw IOException("Couldn't create file")
                calendar.add(Calendar.MINUTE, (-timeout).toInt())

                bufferedWriter().use { out ->
                    out.write(
                        "Appel effectué le ${hdf.format(calendar.time)} " +
                                "par ${event.member?.effectiveName ?: "anonymous"} " +
                                "dans le salon #${event.textChannel.name}\n\n"
                    )
                    out.write("${students.size} ${"personne".pluralize(students.size)} ${"présente".pluralize(students.size)} :\n")
                    out.write(students.joinToString("\n") { " - $it" })
                }

                event.member?.user?.openPrivateChannel()?.queue({
                    it.sendFile(this).queue()
                    onResultSuccess()
                }, {
                    onResultFailed()
                })
            }
        } catch (e: IOException) {
            onResultFailed()
        }
    }

    private fun onResultSuccess() {
        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle("Appel demandé par ${event.member!!.effectiveName}")
                .setDescription("L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${"présente".pluralize(students.size)}.")
                .setColor(Color(0xe67e22))
                .build()
        ).queue()
        event.hook.editOriginalComponents(
            ActionRow.of(
                Button.danger("0", "Appel terminé")
                    .withDisabled(true)
            )
        ).queue()
    }

    private fun onResultFailed() {
        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle("Appel demandé par ${event.member!!.effectiveName}")
                .setDescription(
                    "L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${"présente".pluralize(students.size)}.\n\n" +
                            "**Une erreur est survenue lors de l'envoi du fichier !**"
                )
                .setColor(Color(0xe74c3c))
                .build()
        ).queue()
        event.hook.editOriginalComponents(
            ActionRow.of(
                Button.danger("0", "Appel terminé")
                    .withDisabled(true)
            )
        ).queue()
    }

}
