package io.slama.commands

import io.slama.core.BotConfiguration
import io.slama.core.ConfigFolders
import io.slama.utils.EmbedColors
import io.slama.utils.TaskScheduler
import io.slama.utils.pluralize
import io.slama.utils.replySuccess
import io.slama.utils.replyWarning
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.LoggerFactory

const val DEFAULT_CALL_TIMEOUT = 5L
private val logger = LoggerFactory.getLogger("Call")

class CallCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "call") return
        if (event.guild == null) return
        if (event.member == null) return

        Call(
            event.jda,
            event.member!!,
            event.channel.asTextChannel(),
            event.getOption("timeout")?.asLong ?: DEFAULT_CALL_TIMEOUT,
            event.getOption("role")?.asRole,
            event
        )
    }
}

class Call(
    private val jda: JDA,
    private val member: Member,
    private val channel: TextChannel,
    private val timeout: Long = DEFAULT_CALL_TIMEOUT,
    private val role: Role? = null,
    event: SlashCommandInteractionEvent? = null,
) : ListenerAdapter() {
    private val students = mutableSetOf<String>()
    private val missing = mutableSetOf<String>()
    private val uniqueId = "${member.user.id}${System.currentTimeMillis()}"
    private val embedTitle = "Appel demandé par ${member.effectiveName}"

    private lateinit var responseId: String
    private var totalTargeted: Int = 0

    init {
        jda.addEventListener(this)
        logger.info("$member initiated a call in $channel with a timeout of $timeout minutes")

        role?.guild?.findMembers { role in it.roles }?.onSuccess { members ->
            members.map { it.effectiveName }.forEach { missing.add(it) }
            totalTargeted = members.size
        }

        val embed = EmbedBuilder()
            .setTitle(embedTitle)
            .setDescription("Vous avez $timeout ${"minute".pluralize(timeout.toInt())} pour répondre à l'appel.")
            .setColor(EmbedColors.GREEN)
            .build()

        if (event != null) {
            event.replyEmbeds(embed)
                .addActionRow(Button.success("$uniqueId.respond", "Répondre à l'appel"))
                .queue {
                    it.retrieveOriginal().queue { message -> responseId = message.id }
                    TaskScheduler.later(timeout, TimeUnit.MINUTES, ::sendResult)
                }
        } else {
            channel.sendMessageEmbeds(embed)
                .addActionRow(Button.success("$uniqueId.respond", "Répondre à l'appel"))
                .queue {
                    responseId = it.id
                    TaskScheduler.later(timeout, TimeUnit.MINUTES, ::sendResult)
                }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId != "$uniqueId.respond") return
        event.member?.run {
            if (role != null && role !in roles) {
                event.replyWarning("Vous n'avez pas le rôle requis pour répondre à l'appel.")
                    .setEphemeral(true)
                    .queue()
                return
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
        jda.removeEventListener(this)

        val calendar = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")
        val hdf = SimpleDateFormat("dd/MM/yyyy à HH:mm")
        val fileName =
            "call_${member.effectiveName}_#${channel.name}_${df.format(calendar.time)}.txt"

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
                    |Appel effectué le ${hdf.format(calendar.time)} par ${member.effectiveName} dans le salon #${channel.name}
                    |
                """.trimMargin()
                )
                if (role != null) {
                    printMissing(out)
                }
                printPresents(out)
                out.flush()
            }

            member.user.openPrivateChannel().queue {
                it.sendFiles(FileUpload.fromData(this)).queue({ onResultSuccess() }, { onResultFailed() })
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
        channel.editMessageEmbedsById(
            responseId,
            EmbedBuilder()
                .setTitle(embedTitle)
                .setDescription(resultString())
                .setColor(EmbedColors.ORANGE)
                .build()
        ).queue()
        channel.editMessageComponentsById(
            responseId,
            ActionRow.of(
                Button.danger("0", "Appel terminé").withDisabled(true)
            )
        ).queue()
    }

    private fun onResultFailed() {
        channel.editMessageEmbedsById(
            responseId,
            EmbedBuilder()
                .setTitle("Appel demandé par ${member.effectiveName}")
                .setDescription(resultString())
                .setFooter("Une erreur est survenue lors de l'envoi du fichier !")
                .setColor(EmbedColors.RED)
                .build()
        ).queue()
        channel.editMessageComponentsById(
            responseId,
            ActionRow.of(
                Button.danger("0", "Appel terminé").withDisabled(true)
            )
        ).queue()
    }

    private fun resultString(): String {
        return if (role == null) {
            "L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} étai${if (students.size > 1) "ent" else "t"} ${
                "présente".pluralize(
                    students.size
                )
            }."
        } else {
            "L'appel est terminé. ${students.size} ${"personne".pluralize(students.size)} sur $totalTargeted étai${if (students.size > 1) "ent" else "t"} ${
                "présente".pluralize(
                    students.size
                )
            }."
        }
    }
}
