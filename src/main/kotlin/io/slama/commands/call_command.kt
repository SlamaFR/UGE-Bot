package io.slama.commands

import io.slama.core.BotConfiguration
import io.slama.core.ConfigFolders
import io.slama.utils.EmbedColors
import io.slama.utils.TaskScheduler
import io.slama.utils.isTeacher
import io.slama.utils.pluralize
import io.slama.utils.replySuccess
import io.slama.utils.replyWarning
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val DEFAULT_TIMEOUT = 5L
private val logger = LoggerFactory.getLogger("CallCommand")

class CallCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "call") return
        if (event.guild == null) return
        if (!event.member!!.isTeacher()) return

        Call(
            event,
            event.getOption("timeout")?.asLong ?: DEFAULT_TIMEOUT,
            event.getOption("role")?.asRole
        )
    }
}

private class Call(
    private val event: SlashCommandEvent,
    private val timeout: Long = DEFAULT_TIMEOUT,
    private val role: Role? = null
) : ListenerAdapter() {
    private val students = mutableSetOf<String>()
    private val missing = mutableSetOf<String>()
    private val uniqueId = "${event.user.id}${System.currentTimeMillis()}"
    private val embedTitle = "Appel demandé par ${event.member?.effectiveName ?: "un certain A. N. Onym"}"

    private lateinit var responseId: String
    private var totalTargeted: Int = 0

    init {
        event.jda.addEventListener(this)
        logger.info("${event.member} initiated a call in ${event.channel} with a timeout of $timeout minutes")

        role?.guild?.findMembers { role in it.roles }?.onSuccess { members ->
            members.map { it.effectiveName }.forEach { missing.add(it) }
            totalTargeted = members.size
        }

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle(embedTitle)
                .setDescription("Vous avez $timeout ${"minute".pluralize(timeout.toInt())} pour répondre à l'appel.")
                .setColor(EmbedColors.GREEN)
                .build()
        )
            .addActionRow(Button.success("$uniqueId.respond", "Répondre à l'appel"))
            .queue {
                it.retrieveOriginal().queue { message -> responseId = message.id }
                TaskScheduler.later(timeout, TimeUnit.MINUTES, ::sendResult)
            }
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.componentId != "$uniqueId.respond") return
        event.member?.run {
            if (role != null) {
                if (!roles.contains(role)) {
                    event.replyWarning("Vous n'avez pas le rôle requis pour répondre à l'appel.").setEphemeral(true).queue()
                    return
                }
            }
            if (effectiveName !in students) {
                event.replySuccess("Votre présence a été enregistrée.").setEphemeral(true).queue()
                students.add(effectiveName)
                missing.remove(effectiveName)
            } else {
                event.replyWarning("Vous avez déjà répondu à cet appel !").setEphemeral(true).queue()
            }
        }
    }

    private fun sendResult() {
        event.jda.removeEventListener(this)

        val calendar = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")
        val hdf = SimpleDateFormat("dd/MM/yyyy à HH:mm")
        val fileName =
            "call_${event.member?.effectiveName ?: "anonymous"}_#${event.textChannel.name}_${df.format(calendar.time)}.txt"

        with(File(ConfigFolders.CALLS_DATA_ROOT)) {
            if (!exists() || !isDirectory)
                BotConfiguration.resetConfig()
        }

        File(ConfigFolders.CALLS_DATA_ROOT, fileName).apply {
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
                """.trimMargin()
                )
                if (role != null) {
                    printMissing(out)
                }
                printPresents(out)
                out.flush()
            }

            event.member?.user?.openPrivateChannel()?.queue {
                it.sendFile(this).queue({ onResultSuccess() }, { onResultFailed() })
            } ?: run {
                logger.error("Couldn't created call file '$fileName'")
                onResultFailed()
            }
        }
    }

    private fun printPresents(out: BufferedWriter) {
        if (students.isEmpty()) {
            out.write(
                """
                |
                |Aucune personne présente !
                |
            """.trimMargin()
            )
        } else {
            out.write(
                """
                |
                |${students.size} ${"personne".pluralize(students.size)} ${"présente".pluralize(students.size)} :
                |${students.joinToString("\n") { " - $it" }}
                |
            """.trimMargin()
            )
        }
    }

    private fun printMissing(out: BufferedWriter) {
        if (missing.isEmpty()) {
            out.write(
                """
                |
                |Aucune personne absente !
                |
            """.trimMargin()
            )
        } else {
            out.write(
                """
                |
                |${missing.size} ${"personne".pluralize(missing.size)} ${"absente".pluralize(missing.size)} :
                |${missing.joinToString("\n") { " - $it" }}
                |
            """.trimMargin()
            )
        }
    }

    private fun onResultSuccess() {
        event.channel.editMessageEmbedsById(
            responseId,
            EmbedBuilder()
                .setTitle(embedTitle)
                .setDescription(resultString())
                .setColor(EmbedColors.ORANGE)
                .build()
        ).queue()
        event.channel.editMessageComponentsById(
            responseId,
            ActionRow.of(
                Button.danger("0", "Appel terminé").withDisabled(true)
            )
        ).queue()
    }

    private fun onResultFailed() {
        event.channel.editMessageEmbedsById(
            responseId,
            EmbedBuilder()
                .setTitle("Appel demandé par ${event.member?.effectiveName ?: "un certain A. N. Onym"}")
                .setDescription(
                    """
                        |${resultString()}
                        |
                        |**Une erreur est survenue lors de l'envoi du fichier !**
                    """.trimMargin()
                )
                .setColor(EmbedColors.RED)
                .build()
        ).queue()
        event.channel.editMessageComponentsById(
            responseId,
            ActionRow.of(
                Button.danger("0", "Appel terminé").withDisabled(true)
            )
        ).queue()
    }

    private fun resultString(): String {
        return if (role == null) {
            "L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${
                "présente".pluralize(students.size)
            }."
        } else {
            "L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} sur $totalTargeted étai${if (students.size > 1) "ent" else "t"} ${
                "présente".pluralize(students.size)
            }."
        }
    }
}
