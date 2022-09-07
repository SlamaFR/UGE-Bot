package io.slama.commands

import io.slama.utils.EmbedColors
import io.slama.utils.replyError
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RockPaperScissors")

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

        RockPaperScissors(event, RPSPlayer(player1ID), RPSPlayer(player2ID), rounds).init()
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

    private var currentRound = 1

    private val rockButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-rock", "Pierre", RPSMove.ROCK.emoji
    )
    private val paperButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-paper", "Feuille", RPSMove.PAPER.emoji
    )
    private val scissorsButton = Button.of(
        ButtonStyle.SECONDARY, "rps.$gameId-scissors", "Ciseaux", RPSMove.SCISSORS.emoji
    )

    init {
        event.jda.addEventListener(this)
    }

    fun init() {
        logger.info("[GAME-$gameId] (Round $currentRound/$rounds) Game started between ${player1.id} and ${player2.id}")
        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("Pierre Feuille Ciseaux")
                .setDescription("$player1 a défié $player2 ! Chaque joueur doit jouer son coup.")
                .setFooter("Partie n°$gameId • Round $currentRound/$rounds")
                .setColor(EmbedColors.VIOLET)
                .build()
        ).addActionRow(
            Button.success("rps.$gameId-play", "Jouer")
        ).queue()
    }

    private fun confirmStart(event: ButtonClickEvent) {
        val player = when (event.user.idLong) {
            player1.id -> player1
            player2.id -> player2
            else -> return
        }
        if (player.move != null) {
            event.replyError("Vous avez déjà joué !").setEphemeral(true).queue()
            return
        }
        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("Pierre Feuille Ciseaux")
                .setDescription("Faites votre choix ! La partie sera terminée lorsque tous les joueurs auront joué.")
                .setFooter("Partie n°$gameId • Round $currentRound/$rounds")
                .build()
        ).addActionRow(rockButton, paperButton, scissorsButton)
            .setEphemeral(true)
            .queue()
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
        logger.info("[GAME-$gameId] (Round $currentRound/$rounds) Player ${player.id} played $move")
        event.editComponents(
            ActionRow.of(
                rockButton.asDisabled(),
                paperButton.asDisabled(),
                scissorsButton.asDisabled(),
            )
        ).queue()
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
        val previousRoundSummary = RPSRoundSummary(player1, player2, player1.move!!, player2.move!!, winner)
        roundSummaries.add(previousRoundSummary)

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

        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle("Pierre Feuille Ciseaux")
                .setDescription(
                    "${if (winner != null) "$winner" else "Personne ne"} remporte le tour !\n\n" +
                            "$previousRoundSummary\n\n" +
                            "**Vous pouvez rejouer !**"
                )
                .setFooter("Partie n°$gameId • Round $currentRound/$rounds")
                .setColor(EmbedColors.ORANGE)
                .build()
        ).queue()

        currentRound++
        player1.move = null
        player2.move = null
    }

    private fun end(winner: RPSPlayer?) {
        logger.info("[GAME-$gameId] (Round $currentRound/$rounds) Game ended, winner: ${winner?.id}")
        event.jda.addEventListener(this)
        event.hook.editOriginalEmbeds(
            EmbedBuilder()
                .setTitle("Pierre Feuille Ciseaux")
                .setDescription(
                    "${if (winner != null) "$winner" else "Personne ne"} remporte la partie !\n\n" +
                            "**La partie est terminée !**\n"
                )
                .addField(
                    "Résumé des rounds",
                    roundSummaries.joinToString("\n") { it.toString() },
                    false
                )
                .addField(
                    "Score final",
                    "$player1 ${player1.score} - ${player2.score} $player2",
                    false
                )
                .setFooter("Partie n°$gameId • Round $currentRound/$rounds")
                .setColor(if (winner != null) EmbedColors.GREEN else EmbedColors.RED)
                .build()
        ).setActionRows().queue()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.user.idLong != player1.id && event.user.idLong != player2.id) {
            event.replyError("Vous n'êtes pas un joueur de cette partie !").queue()
            return
        }

        when (event.componentId) {
            "rps.$gameId-play" -> confirmStart(event)
            "rps.$gameId-rock" -> play(event, RPSMove.ROCK)
            "rps.$gameId-paper" -> play(event, RPSMove.PAPER)
            "rps.$gameId-scissors" -> play(event, RPSMove.SCISSORS)
        }
    }
}

data class RPSRoundSummary(
    val player1: RPSPlayer,
    val player2: RPSPlayer,
    val player1Move: RPSMove,
    val player2Move: RPSMove,
    val winner: RPSPlayer?
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
