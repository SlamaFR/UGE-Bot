package events

import core.AutoRoleDTO
import core.getConfigOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val autoRoles = mutableMapOf<Long, MutableMap<String, AutoRole>>()
val logger: Logger = LoggerFactory.getLogger("AutoRolesManager")

fun createAutoRoleIfAbsent(guild: Guild, name: String, config: AutoRoleDTO): AutoRole? {
    if (guild.idLong !in autoRoles.keys) {
        autoRoles[guild.idLong] = mutableMapOf()
    }

    val guildAutoRoles = autoRoles.getOrPut(guild.idLong) { mutableMapOf() }

    if (name !in guildAutoRoles.keys) {
        val autoRole = AutoRole(config, name, guild.jda)
        guildAutoRoles[name] = autoRole
        logger.info("Successfully created AutoRole \"$name\" on ${guild.id}")
        return autoRole
    }
    return guildAutoRoles[name]
}

fun createAutoRoleIfAbsent(guild: Guild, name: String): AutoRole? {
    val config = guild.getConfigOrNull() ?: return null
    val autoRoleDTO = config.autoRoles[name] ?: return null
    return createAutoRoleIfAbsent(guild, name, autoRoleDTO)
}

fun Guild.loadAutoRoles() {
    val config = this.getConfigOrNull() ?: return
    config.autoRoles.forEach { (name, autoRole) ->
        createAutoRoleIfAbsent(this, name, autoRole)
    }
}

class AutoRole(
    private val config: AutoRoleDTO,
    private val name: String,
    jda: JDA,
) : ListenerAdapter() {

    private val roles = config.roles.map { it.role }

    init {
        jda.addEventListener(this)
    }

    fun send(textChannel: TextChannel) {
        textChannel.sendMessage(
            EmbedBuilder()
                .setTitle(config.title)
                .setDescription(config.description)
                .setColor(config.color)
                .build()
        ).apply {
            for (i in roles.indices.chunked(5)) {
                this.setActionRows(ActionRow.of(
                    i.associateWith {
                        config.roles[it]
                    }.map {
                        Button.of(it.value.color, "${name}.${it.key}", it.value.title)
                    }
                ))
            }
        }.queue()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (!event.componentId.startsWith(name)) return
        val guild = event.guild ?: return
        val member = event.member ?: return

        val index = event.componentId.split(".")[1].toInt()
        if (index >= roles.size) return

        val role = guild.getRoleById(roles[index])
        if (role == null) {
            event.reply("**Erreur :** Le rôle demandé est introuvable. Contactez l'administrateur.")
                .setEphemeral(true)
                .queue()
            return
        }
        if (role in member.roles) return

        guild.addRoleToMember(event.user.id, role).queue {
            event.reply(":white_check_mark: Le rôle ${role.asMention} vous a été attribué !")
                .setEphemeral(true)
                .queue()
        }
    }
}
