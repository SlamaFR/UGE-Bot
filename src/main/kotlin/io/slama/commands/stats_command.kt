package io.slama.commands

import io.slama.core.BotConfiguration
import io.slama.games.GameStatistics
import io.slama.utils.pluralize
import io.slama.utils.replyError
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.Locale

class StatsCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "stats") return
        val guild = event.guild ?: return

        val emoteConfig = BotConfiguration.emotes
        val V = emoteConfig.victory
        val N = emoteConfig.draw
        val D = emoteConfig.defeat

        val player = event.getOption("player")?.asUser ?: event.user
        val playerName = guild.getMemberById(player.idLong)?.effectiveName ?: player.name
        val statsFile = RPS_GAME_STATISTICS_FOLDER.resolve("${player.id}.json").toFile()

        if (!statsFile.exists()) {
            event.replyError("Ce joueur ne possède aucune statistique.").setEphemeral(true).queue()
            return
        }

        val statistics = Json.decodeFromString(GameStatistics.serializer(), statsFile.readText())
        val lostGames = statistics.playedGames - statistics.wonGames
        val lostRounds = statistics.playedRounds - statistics.wonRounds - statistics.tiedRounds

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("Statistiques de $playerName")
                .addField(
                    "Parties jouées",
                    "${statistics.playedGames} (${statistics.startedGames} ${"lancée".pluralize(statistics.startedGames)})",
                    true
                )
                .addField(
                    "Détails des parties",
                    "$V${statistics.wonGames} $D$lostGames",
                    true
                )
                .addField(
                    "Ratio de victoire",
                    ratio(statistics.wonGames, lostGames),
                    true
                )
                .addField(
                    "Tours joués",
                    "${statistics.playedRounds}",
                    true
                )
                .addField(
                    "Détails des tours",
                    "$V${statistics.wonRounds} $D$lostRounds $N${statistics.tiedRounds}",
                    true
                )
                .addField(
                    "Ratio de victoire",
                    ratio(statistics.wonRounds, lostRounds),
                    true
                )
                .build()
        ).queue()
    }

    private fun ratio(victories: Int, defeats: Int): String {
        return if (defeats > 0) {
            String.format(Locale.FRENCH, "%.2f", victories.toDouble() / defeats.toDouble())
        } else {
            ":fire:"
        }
    }
}
