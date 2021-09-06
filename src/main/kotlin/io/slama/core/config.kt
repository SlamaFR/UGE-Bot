package io.slama.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.io.File
import java.io.IOException

private const val BOT_PROPERTIES = "bot.properties"
private const val CONFIG_ROOT = "config/"
private const val GUILD_CONFIG_ROOT = "${CONFIG_ROOT}guilds/"
private val guildConfigs: MutableMap<Long, GuildConfig> = mutableMapOf()

fun configSetup() {
    with(File(BOT_PROPERTIES)) {
        if (!this.exists())
            writeText("token=0\n")
        else if (!this.isFile)
            throw IOException("Couldn't create a $BOT_PROPERTIES file as a folder with that name already exists.")
    }
    with(File(CONFIG_ROOT)) {
        mkdir()
        if (this.isDirectory) with(File(GUILD_CONFIG_ROOT)) {
            mkdir()
            if (!this.isDirectory)
                throw IOException("Couldn't create the $GUILD_CONFIG_ROOT directory as there is already a file named like that.")
        }
        else
            throw IOException("Couldn't create the $CONFIG_ROOT directory as there is already a file named like that.")

    }
}

@Serializable
data class RolesDTO(
    val adminRoleID: Long,
    val managerRoleID: Long,
    val teacherRoleID: Long,
    val studentRoleID: Long,
)

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
data class ShusherDTO(
    val sentences: List<String>
)

@Serializable
data class PresenceDTO(
    val messages: Map<String, Activity.ActivityType>
)

data class GuildConfig(
    val roles: RolesDTO,
    val channels: ChannelsDTO,
    val autoRoles: Map<String, AutoRoleDTO>
)

fun getConfigOrNull(guildId: Long): GuildConfig? {
    if (guildId !in guildConfigs.keys) loadConfig(guildId)
    return guildConfigs[guildId]
}

fun Guild.getConfigOrNull(): GuildConfig? = getConfigOrNull(this.idLong)

fun clearGuildConfigs() {
    guildConfigs.clear()
}

private fun createRolesFile(file: File) {
    file.createNewFile()
    file.writeText(
        """
        {
          "adminRoleID": 0,
          "managerRoleID": 1,
          "teacherRoleID": 2,
          "studentRoleID": 3
        }
    """.trimIndent()
    )
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

@OptIn(ExperimentalSerializationApi::class)
private fun loadConfig(guildId: Long) {
    File("$GUILD_CONFIG_ROOT$guildId").run {
        mkdir()
        if (!this.isDirectory)
            throw IOException("Couldn't create the $name directory as there is already a file named like that.")
    }

    val rolesF = File("$GUILD_CONFIG_ROOT$guildId/roles.json")
    if (!rolesF.exists()) {
        createRolesFile(rolesF)
    }

    val channelsF = File("$GUILD_CONFIG_ROOT$guildId/channels.json")
    if (!channelsF.exists()) {
        createChannelsFile(channelsF)
    }

    val autorolesF = File("$GUILD_CONFIG_ROOT$guildId/autoroles.json")
    if (!autorolesF.exists()) {
        createAutorolesFile(autorolesF)
    }

    guildConfigs[guildId] = GuildConfig(
        Json.decodeFromString(rolesF.readText()),
        Json.decodeFromString(channelsF.readText()),
        Json.decodeFromString(autorolesF.readText()),
    )
}

@OptIn(ExperimentalSerializationApi::class)
fun getShusherConfig(): ShusherDTO {
    val shusherF = File("${CONFIG_ROOT}shusher.json")
    if (!shusherF.exists()) {
        createShusherFile(shusherF)
    }
    return Json.decodeFromString(shusherF.readText())
}

@OptIn(ExperimentalSerializationApi::class)
fun getPresenceConfig(): PresenceDTO {
    val presenceF = File("${CONFIG_ROOT}presence.json")
    if (!presenceF.exists()) {
        createPresenceFile(presenceF)
    }
    return Json.decodeFromString(presenceF.readText())
}
