package io.slama.core

import io.slama.core.ConfigFolders.BOT_PROPERTIES
import io.slama.core.ConfigFolders.CALLS_DATA_ROOT
import io.slama.core.ConfigFolders.CONFIG_ROOT
import io.slama.core.ConfigFolders.DATA_ROOT
import io.slama.core.ConfigFolders.GAMES_DATA_ROOT
import io.slama.core.ConfigFolders.GUILD_CONFIG_ROOT
import io.slama.core.ConfigFolders.POLLS_DATA_ROOT
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlinx.serialization.encodeToString

private val logger = LoggerFactory.getLogger("Configuration")

object ConfigFolders {
    const val BOT_PROPERTIES = "bot.properties"
    const val CONFIG_ROOT = "config/"
    const val DATA_ROOT = "data/"
    const val CALLS_DATA_ROOT = "${DATA_ROOT}calls/"
    const val POLLS_DATA_ROOT = "${DATA_ROOT}polls/"
    const val GAMES_DATA_ROOT = "${DATA_ROOT}games/"
    const val GUILD_CONFIG_ROOT = "${CONFIG_ROOT}guilds/"
}

class BotConfiguration private constructor() {

    companion object {
        val guilds: GuildConfigManager
            get() = innerConfig!!.guildConfigs /*
            Explicitly want an NPE if it's null here,
            because it can only happen if the JVM is literally dying
            */

        val shusher: ShusherConfig?
            get() = innerConfig!!.shusherConfig /*
            Explicitly want an NPE if it's null here,
            because it can only happen if the JVM is literally dying
            */

        val emotes: EmotesConfig
            get() = innerConfig!!.emotesConfig /*
            Explicitly want an NPE if it's null here,
            because it can only happen if the JVM is literally dying
            */

        val presence: PresenceConfig?
            get() = innerConfig!!.presenceConfig /*
            Explicitly want an NPE if it's null here,
            because it can only happen if the JVM is literally dying
            */

        val mail: MailConfig?
            get() = innerConfig!!.mailConfig /*
            Explicitly want an NPE if it's null here,
            because it can only happen if the JVM is literally dying
            */

        private var innerConfig: BotConfiguration? = null
            get() {
                if (field == null)
                    field = BotConfiguration()
                return field
            }

        private var backupGuilds: GuildConfigManager? = null
        private var backupShusher: ShusherConfig? = null
        private var backupEmotes: EmotesConfig? = null
        private var backupPresence: PresenceConfig? = null
        private var backupMail: MailConfig? = null

        fun resetConfig() {
            setup()
            backupGuilds = guilds
            backupShusher = shusher
            backupEmotes = emotes
            backupPresence = presence
            backupMail = mail
            innerConfig = null
        }

        private fun setup() {
            with(File(BOT_PROPERTIES)) {
                if (!exists())
                    writeText("token=0\n")
                else if (!isFile)
                    throw IOException("Couldn't create a $BOT_PROPERTIES file.")
            }
            with(File(CONFIG_ROOT)) {
                mkdir()
                if (!isDirectory) throw IOException("Couldn't create the $CONFIG_ROOT directory.")
                else with(File(GUILD_CONFIG_ROOT)) {
                    mkdir()
                    if (!isDirectory)
                        throw IOException("Couldn't create the $GUILD_CONFIG_ROOT directory.")
                }
            }
            setupDataFolder()
        }

        private fun setupDataFolder() {
            with(File(DATA_ROOT)) {
                mkdir()
                if (!isDirectory) throw IOException("Couldn't create the $DATA_ROOT directory.")
                else {
                    with(File(CALLS_DATA_ROOT)) {
                        mkdir()
                        if (!isDirectory) throw IOException("Couldn't create the $CALLS_DATA_ROOT directory.")
                    }
                    with(File(POLLS_DATA_ROOT)) {
                        mkdir()
                        if (!isDirectory) throw IOException("Couldn't create the $POLLS_DATA_ROOT directory.")
                    }
                    with(File(GAMES_DATA_ROOT)) {
                        mkdir()
                        if (!isDirectory) throw IOException("Couldn't create the $GAMES_DATA_ROOT directory.")
                    }
                }
            }
        }

        private fun createChannelsFile(file: File) {
            file.createNewFile()
            file.writeText(
                """
                {
                  "announcementsChannelID": 0,
                  "moodleAnnouncementsChannelsIDs": {
                  }
                }
                """.trimIndent()
            )
        }

        private fun createAutorolesFile(file: File) {
            file.createNewFile()
            file.writeText("{}\n")
        }

        private fun createScheduledCallsFile(file: File) {
            file.createNewFile()
            file.writeText("[]\n")
        }

        private fun createShusherFile(file: File) {
            file.createNewFile()
            file.writeText(
                """
                {
                  "sentences": [
                    "Please stop talking..."
                  ]
                }
                """.trimIndent()
            )
        }

        private fun createEmotesFile(file: File) {
            file.createNewFile()
            file.writeText("{}\n")
        }

        private fun createPresenceFile(file: File) {
            file.createNewFile()
            file.writeText(
                """
                {
                  "messages": {
                    "something": "DEFAULT",
                    "something else": "WATCHING",
                    "some other thing": "LISTENING"
                  }
                }
                """.trimIndent()
            )
        }

        private fun createMailFile(file: File) {
            file.createNewFile()
            file.writeText(
                """
                {
                  "hostname": "mail.server.com",
                  "port": 993,
                  "username": "john.doe@server.com",
                  "password": "1234"
                }
                """.trimIndent()
            )
        }
    }

    class GuildConfigManager {

        private val guildConfigsMap: MutableMap<Long, GuildConfig> = mutableMapOf()

        operator fun get(guildId: Long): GuildConfig? {
            if (guildId !in guildConfigsMap.keys) {
                val old = backupGuilds?.guildConfigsMap?.get(guildId)
                try {
                    loadConfig(guildId)
                } catch (e: SerializationException) {
                    logger.warn("$guildId: Failed to load guild config. Using backup")
                    if (old != null) {
                        guildConfigsMap[guildId] = old
                    } else {
                        logger.error("$guildId: No backup! Guild will be ignored")
                        return null
                    }
                }
            }
            return guildConfigsMap[guildId]
        }

        private fun loadConfig(guildId: Long) {
            with(File(GUILD_CONFIG_ROOT)) {
                if (!exists() || !isDirectory)
                    resetConfig()
            }
            File("$GUILD_CONFIG_ROOT$guildId").run {
                mkdir()
                if (!this.isDirectory)
                    throw IOException("Couldn't create the $name directory as there is already a file named like that.")
            }

            val channelsF = File("$GUILD_CONFIG_ROOT$guildId/channels.json")
            if (!channelsF.exists()) {
                createChannelsFile(channelsF)
            }

            val autorolesF = File("$GUILD_CONFIG_ROOT$guildId/autoroles.json")
            if (!autorolesF.exists()) {
                createAutorolesFile(autorolesF)
            }

            val scheduledCallsF = File("$GUILD_CONFIG_ROOT$guildId/scheduledCalls.json")
            if (!scheduledCallsF.exists()) {
                createScheduledCallsFile(scheduledCallsF)
            }

            guildConfigsMap[guildId] = GuildConfig(
                Json.decodeFromString(channelsF.readText()),
                Json.decodeFromString(autorolesF.readText()),
                Json.decodeFromString(scheduledCallsF.readText()),
            )
            logger.info("$guildId: Loaded config.")
        }
    }

    private val guildConfigs: GuildConfigManager by lazy {
        GuildConfigManager()
    }

    private val shusherConfig: ShusherConfig? by lazy {
        with(File(CONFIG_ROOT)) {
            if (!exists() || !isDirectory)
                resetConfig()
        }
        val shusherF = File("${CONFIG_ROOT}shusher.json")
        if (!shusherF.exists()) {
            createShusherFile(shusherF)
        }

        var config: ShusherConfig?
        try {
            config = Json.decodeFromString(shusherF.readText())
            logger.info("Loaded shusher config")
        } catch (e: SerializationException) {
            logger.warn("Failed to load shusher config. Using backup")
            config = if (backupShusher != null) {
                backupShusher
            } else {
                logger.warn("No backup for shusher config. Using default")
                null
            }
        }
        config
    }

    private val emotesConfig: EmotesConfig by lazy {
        with(File(CONFIG_ROOT)) {
            if (!exists() || !isDirectory)
                resetConfig()
        }
        val emotesF = File("${CONFIG_ROOT}emotes.json")
        if (!emotesF.exists()) {
            createEmotesFile(emotesF)
        }

        var config: EmotesConfig
        try {
            config = Json.decodeFromString(emotesF.readText())
            logger.info("Loaded emotes config")
        } catch (e: SerializationException) {
            logger.warn("Failed to load emotes config. Using backup")
            config = backupEmotes ?: run {
                logger.warn("No backup for emotes config. Using default")
                EmotesConfig()
            }
        }
        config
    }

    private val presenceConfig: PresenceConfig? by lazy {
        with(File(CONFIG_ROOT)) {
            if (!exists() || !isDirectory)
                resetConfig()
        }
        val presenceF = File("${CONFIG_ROOT}presence.json")
        if (!presenceF.exists()) {
            createPresenceFile(presenceF)
        }
        var config: PresenceConfig?
        try {
            config = Json.decodeFromString(presenceF.readText())
            logger.info("Loaded presence config")
        } catch (e: SerializationException) {
            logger.warn("Failed to load presence config. Using backup")
            config = if (backupPresence != null) {
                backupPresence
            } else {
                logger.warn("No backup for presence config. Using default")
                null
            }
        }
        config
    }

    private val mailConfig: MailConfig? by lazy {
        with(File(CONFIG_ROOT)) {
            if (!exists() || !isDirectory)
                resetConfig()
        }
        val mailF = File("${CONFIG_ROOT}mail.json")
        if (!mailF.exists()) {
            createMailFile(mailF)
        }
        var config: MailConfig?
        try {
            config = Json.decodeFromString(mailF.readText())
            logger.info("Loaded mail config")
        } catch (e: SerializationException) {
            logger.warn("Failed to load mail config. Using backup.")
            config = if (backupMail != null) {
                backupMail
            } else {
                logger.error("No backup for mail config. Mail functionality will be disabled.")
                null
            }
        }
        config
    }
}

@Serializable
data class ChannelsDTO(
    val announcementsChannelID: Long,
    val moodleAnnouncementsChannelsIDs: Map<String, Long>,
)

@Serializable
data class AutoRoleButtonDTO(
    val title: String,
    val role: Long,
    val emote: String? = null,
    val color: ButtonStyle = ButtonStyle.PRIMARY,
)

@Serializable
data class AutoRoleDTO(
    val title: String,
    val description: String,
    val color: Int,
    val roles: List<AutoRoleButtonDTO>,
    val maxRoles: Int = 1,
)

@Serializable
data class ScheduledCallDTO(
    val channelId: Long,
    val recipientId: Long,
    val roleId: Long? = null,
    val timeout: Long? = 5,
    val executionTime: String,
)

@Serializable
data class ShusherConfig(
    val sentences: List<String>,
)

@Serializable
data class EmotesConfig(
    val victory: String? = ":green_square: ",
    val draw: String? = ":orange_square: ",
    val defeat: String? = ":red_square: ",
    val greenDot: String? = ":green_circle:",
    val orangeDot: String? = ":orange_circle:",
    val redDot: String? = ":red_circle:",
)

@Serializable
data class PresenceConfig(
    val messages: Map<String, Activity.ActivityType>,
)

@Serializable
data class MailConfig(
    val hostname: String,
    val port: Int,
    val username: String,
    val password: String,
    val debugMode: Boolean = false,
    val enableSSL: Boolean = true,
)

data class GuildConfig(
    val channels: ChannelsDTO,
    val autoRoles: Map<String, AutoRoleDTO>,
    val scheduledCalls: List<ScheduledCallDTO>,
)
