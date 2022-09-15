package io.slama.games

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path

@Serializable
data class GameStatistics(
    var startedGames: Int = 0,
    var playedGames: Int = 0,
    var wonGames: Int = 0,
    var playedRounds: Int = 0,
    var wonRounds: Int = 0,
    var tiedRounds: Int = 0,
)

interface StatisticsTracker {

    fun save()

    fun registerStartedGame()

    fun registerPlayedGame()

    fun registerWonGame()

    fun registerPlayedRound()

    fun registerWonRound()

    fun registerTiedRound()
}

abstract class AbstractStatisticsTracker(
    rootFolder: Path,
    userId: Long,
) : StatisticsTracker {

    private val userFile: File = rootFolder.resolve("$userId.json").toFile()
    private val userStatistics: GameStatistics = loadUserStatistics()

    init {
        if (!rootFolder.toFile().exists()) {
            rootFolder.toFile().mkdirs()
        }
        if (!userFile.exists()) {
            userFile.createNewFile()
        }
    }

    private fun loadUserStatistics(): GameStatistics {
        return if (userFile.length() == 0L) {
            GameStatistics()
        } else {
            Json.decodeFromString(GameStatistics.serializer(), userFile.readText())
        }
    }

    override fun save() {
        userFile.writeText(Json.encodeToString(GameStatistics.serializer(), userStatistics))
    }

    override fun registerStartedGame() {
        userStatistics.startedGames++
    }

    override fun registerPlayedGame() {
        userStatistics.playedGames++
    }

    override fun registerWonGame() {
        userStatistics.wonGames++
    }

    override fun registerPlayedRound() {
        userStatistics.playedRounds++
    }

    override fun registerWonRound() {
        userStatistics.wonRounds++
    }

    override fun registerTiedRound() {
        userStatistics.tiedRounds++
    }
}
