package io.slama.events

import io.slama.core.BotConfiguration
import io.slama.core.ConfigFolders
import io.slama.utils.isAdmin
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random

private const val SHUSHER_TRIGGER_THRESHOLD = .009
private const val SHUSHER_FILE = "shusherRolesIds"

private val logger: Logger = LoggerFactory.getLogger("Shusher")

class Shusher(
    val jda: JDA
) : ListenerAdapter() {

    private val roles = mutableMapOf<String, String>()
    private val random = Random(System.currentTimeMillis())
    private val config = BotConfiguration.shusher

    init {
        jda.addEventListener(this)
        val file = File(ConfigFolders.DATA_ROOT, SHUSHER_FILE)
        if (file.exists()) {
            file.useLines { lines ->
                lines.map {
                    it.trim('\n').split("\t")
                }.forEach {
                    roles[it[0]] = it[1]
                }
            }
            logger.info("Successfully gathered shusher roles ids")
        }
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.member?.let { member ->
            if (event.message.contentRaw.startsWith("!tg") && member.isAdmin()) {
                command(event)
                return
            }

            if (member.roles.map { it.id }.any { roles[event.guild.id] == it }) {
                if (random.nextFloat() <= SHUSHER_TRIGGER_THRESHOLD) {
                    event.channel.sendMessage(config.sentences.random()).queue()
                }
            }
        }
    }

    private fun command(event: GuildMessageReceivedEvent) {
        getOrCreateRole(event.guild).let { role ->
            if (event.message.mentionedMembers.isEmpty()) {
                event.channel.sendMessage("Qui est la cible chef ?").queue()
                return
            }
            event.message.mentionedMembers.forEach { member ->
                if (member.roles.contains(role)) {
                    event.guild.removeRoleFromMember(member, role).queue()
                    event.channel.sendMessage("C'est bon ${member.asMention}, je te laisse tranquille...").queue()
                } else {
                    event.guild.addRoleToMember(member, role).queue()
                    event.channel.sendMessage("Aller c'est parti, ${member.asMention} je tiens ta veste !").queue()
                }
            }
        }
    }

    private fun getOrCreateRole(guild: Guild): Role {
        if (guild.id in roles.keys) {
            roles[guild.id]?.let {
                guild.getRoleById(it)?.let { role ->
                    return role
                }
            }
        }
        val role = guild.createRole()
            .setName("Shushee")
            .setMentionable(false)
            .setHoisted(false)
            .complete()
        roles[guild.id] = role.id
        save()
        return role
    }

    private fun save() {
        val file = File(ConfigFolders.DATA_ROOT, SHUSHER_FILE)
        if (!file.exists()) file.createNewFile()
        file.writeText("")
        roles.forEach { (guildId, roleId) ->
            file.appendText("$guildId\t$roleId\n")
        }
    }
}
