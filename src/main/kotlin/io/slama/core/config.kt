package io.slama.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

private const val GUILD_CONFIG_ROOT = "config/guilds/"
private val guildConfigs: MutableMap<Long, GuildConfig> = mutableMapOf()

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

data class GuildConfig(
    val roles: RolesDTO,
    val channels: ChannelsDTO,
    val autoRoles: Map<String, AutoRoleDTO>,
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

@OptIn(ExperimentalSerializationApi::class)
private fun loadConfig(guildId: Long) {
    val configDir = Path("$GUILD_CONFIG_ROOT$guildId")
    if (!configDir.exists()) {
        configDir.createDirectory()
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
        Json.decodeFromString(autorolesF.readText())
    )
}