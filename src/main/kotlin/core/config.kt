package core

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.components.ButtonStyle
import java.io.File

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

private fun loadConfig(guildId: Long) {
    guildConfigs[guildId] = GuildConfig(
        Json.decodeFromString(File("$GUILD_CONFIG_ROOT$guildId/roles.json").readText()),
        Json.decodeFromString(File("$GUILD_CONFIG_ROOT$guildId/channels.json").readText()),
        Json.decodeFromString(File("$GUILD_CONFIG_ROOT$guildId/autoroles.json").readText()),
    )
}
