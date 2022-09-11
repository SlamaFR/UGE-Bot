package io.slama.commands

import io.slama.utils.EmbedColors
import io.slama.utils.replyError
import io.slama.utils.replySuccess
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import org.slf4j.LoggerFactory
import java.util.Objects
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("RockPaperScissors")

const val GAME_NAME = "Pierre Feuille Ciseaux"
const val GAME_TIMEOUT = 120L

class RockPaperScissorsCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "pfc") return
        if (event.guild == null) return

        val player1ID = event.user.idLong
        val player2ID = event.getOption("opponent")?.asUser?.idLong ?: return
        val rounds = event.getOption("rounds")?.asLong?.toInt() ?: 1

        if (player1ID == player2ID) {
            event.replyError("Vous ne pouvez pas jouer contre vous-même !")
                .setEphemeral(true)
                .queue()
            return
        }

        RockPaperScissors(
            event,
            RPSPlayer(player1ID),
            RPSPlayer(player2ID),
            if (rounds % 2 == 0) rounds + 1 else rounds
        ).init()
    }
}

class RockPaperScissors(
    private val event: GenericInteractionCreateEvent,
    private val player1: RPSPlayer,
    private val player2: RPSPlayer,
    private val rounds: Int = 1
) : ListenerAdapter() {

    private val gameId = System.currentTimeMillis()
    private val roundSummaries = mutableListOf<RPSRoundSummary>()

    private val rockButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-rock", "Pierre", RPSMove.ROCK.emoji
    )
    private val paperButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-paper", "Feuille", RPSMove.PAPER.emoji
    )
    private val scissorsButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-scissors", "Ciseaux", RPSMove.SCISSORS.emoji
    )

    private var currentRound = 1
    private var cancellationTask: ScheduledFuture<*>? = null

    init {
        event.jda.addEventListener(this)
    }

    fun init() {
        logger.info("[GAME-${gameHash()}] (Round $currentRound/$rounds) Game started between ${player1.id} and ${player2.id}")
        event.replyEmbeds(
            EmbedBuilder()
                .setTitle(GAME_NAME)
                .setDescription(
                    "$player1 a défié $player2 !\n\n" +
                        "**Chaque joueur doit jouer son coup.**"
                )
                .setFooter("Partie #${gameHash()} • Round $currentRound/$rounds • En cours")
                .setColor(EmbedColors.VIOLET)
                .build()
        ).addActionRow(
            rockButton, paperButton, scissorsButton
        ).queue { scheduleTimeout() }
    }

    private fun play(event: ButtonClickEvent, move: RPSMove) {
        val player = when (event.user.idLong) {
            player1.id -> player1
            player2.id -> player2
            else -> return
        }
        if (player.move != null) {
            event.replyError("Vous avez déjà joué !").setEphemeral(true).queue()
            return
        }

        player.move = move
        logger.info("[GAME-${gameHash()}] (Round $currentRound/$rounds) Player ${player.id} played $move")

        event.replySuccess("Vous avez joué $move !")
            .setEphemeral(true)
            .queue()

        scheduleTimeout()
        check()
    }

    private fun check() {
        val player1Move = player1.move ?: return
        val player2Move = player2.move ?: return

        if (player1Move.defeats(player2Move)) {
            player1.score++
            nextRound(player1)
        } else if (player2Move.defeats(player1Move)) {
            player2.score++
            nextRound(player2)
        } else {
            nextRound(null)
        }
    }

    private fun nextRound(winner: RPSPlayer?) {
        val previousRoundSummary = RPSRoundSummary(player1, player2, player1.move!!, player2.move!!)
        if (winner != null) roundSummaries.add(previousRoundSummary)

        if (winner != null && winner.score > rounds / 2) {
            end(winner)
            return
        }

        if (currentRound == rounds) {
            if (player1.score > player2.score) {
                end(player1)
            } else if (player2.score > player1.score) {
                end(player2)
            } else {
                end(null)
            }
            return
        }

        if (winner != null) currentRound++

        player1.move = null
        player2.move = null

        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle(GAME_NAME)
                .setDescription(
                    "${if (winner != null) "$winner" else "Personne ne"} remporte le tour !\n\n" +
                        "$previousRoundSummary\n\n" +
                        "**Vous pouvez rejouer !**"
                )
                .setFooter("Partie #${gameHash()} • Round $currentRound/$rounds • En cours")
                .setColor(EmbedColors.VIOLET)
                .build()
        ).queue()
    }

    private fun end(winner: RPSPlayer?) {
        logger.info("[GAME-${gameHash()}] (Round $currentRound/$rounds) Game ended, winner: ${winner?.id}")

        cancellationTask?.cancel(true)
        event.jda.removeEventListener(this)
        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle(GAME_NAME)
                .setDescription("${if (winner != null) "$winner" else "Personne ne"} remporte la partie !\n\n")
                .addField(
                    "Résumé des tours",
                    roundSummaries.joinToString("\n") { it.toString() },
                    false
                )
                .addField(
                    "Score final",
                    "$player1 ${player1.score} - ${player2.score} $player2",
                    false
                )
                .setFooter("Partie #${gameHash()} • Round $currentRound/$rounds • Terminée")
                .setColor(if (winner != null) EmbedColors.GREEN else EmbedColors.ORANGE)
                .build()
        ).setActionRows().queue()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.user.idLong != player1.id && event.user.idLong != player2.id) {
            event.replyError("Vous n'êtes pas un joueur de cette partie !")
                .setEphemeral(true)
                .queue()
            return
        }

        when (event.componentId) {
            "rps.$gameId-rock" -> play(event, RPSMove.ROCK)
            "rps.$gameId-paper" -> play(event, RPSMove.PAPER)
            "rps.$gameId-scissors" -> play(event, RPSMove.SCISSORS)
        }
    }

    private fun gameHash() = Integer.toHexString(Objects.hash(gameId, player1, player2))

    private fun scheduleTimeout() {
        cancellationTask?.cancel(true)
        cancellationTask = event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle(GAME_NAME)
                .setDescription("La partie a été annulée car un des joueurs n'a pas répondu à temps.")
                .setFooter("Partie #${gameHash()} • Round $currentRound/$rounds • Annulée")
                .setColor(EmbedColors.RED)
                .build()
        ).setActionRows().queueAfter(GAME_TIMEOUT, TimeUnit.SECONDS) {
            event.jda.removeEventListener(this)
        }
    }
}

data class RPSRoundSummary(
    val player1: RPSPlayer,
    val player2: RPSPlayer,
    val player1Move: RPSMove,
    val player2Move: RPSMove,
) {
    override fun toString(): String {
        return "$player1 $player1Move - $player2Move $player2"
    }
}

enum class RPSMove(
    val emoji: Emoji,
    val defeats: (RPSMove) -> Boolean
) {
    ROCK(Emoji.fromUnicode("✊"), { it == SCISSORS }),
    PAPER(Emoji.fromUnicode("✋"), { it == ROCK }),
    SCISSORS(Emoji.fromUnicode("✌️"), { it == PAPER });

    override fun toString(): String {
        return emoji.asMention
    }
}

class RPSPlayer(
    val id: Long
) {
    var move: RPSMove? = null
    var score = 0

    override fun toString(): String {
        return "<@$id>"
    }
}
