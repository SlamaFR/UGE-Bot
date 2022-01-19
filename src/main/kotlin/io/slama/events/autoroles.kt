package io.slama.events

import io.slama.core.AutoRoleDTO
import io.slama.core.BotConfiguration
import io.slama.utils.replyError
import io.slama.utils.replySuccess
import io.slama.utils.replyWarning
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

private val autoRoles = mutableMapOf<Long, MutableMap<String, AutoRole>>()
private val logger: Logger = LoggerFactory.getLogger("AutoRolesManager")

class AutoRole(
    private val config: AutoRoleDTO,
    private val name: String,
    private val guildId: String,
    val jda: JDA,
) : ListenerAdapter() {

    private val roles = config.roles.map { it.role }

    init {
        jda.addEventListener(this)
    }

    fun send(textChannel: TextChannel) {
        textChannel.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle(config.title)
                .setDescription(config.description)
                .setColor(config.color)
                .build()
        ).apply {
            for (i in roles.indices.chunked(5)) {
                this.setActionRows(
                    ActionRow.of(
                        i.associateWith {
                            config.roles[it]
                        }.map {
                            Button.of(it.value.color, "$name.${it.key}", it.value.title)
                        }
                    )
                )
            }
        }.queue()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (!event.componentId.startsWith(name)) return
        val guild = event.guild ?: return
        if (guild.id != guildId) return
        val member = event.member ?: return

        val index = event.componentId.split(".")[1].toInt()
        if (index >= roles.size) return

        val role = guild.getRoleById(roles[index])
        if (role == null) {
            event.replyError("Le rôle demandé est introuvable. Contactez l'administrateur.").setEphemeral(true).queue()
            return
        }
        if (role in member.roles) {
            event.replyWarning("Vous avez déjà reçu ce rôle !").setEphemeral(true).queue()
            return
        }

        guild.addRoleToMember(event.user.id, role).queue {
            event.replySuccess("Le rôle ${role.asMention} vous a été attribué !").setEphemeral(true).queue()
        }
    }
}

fun Guild.createAutoRoleIfAbsent(name: String, config: AutoRoleDTO): AutoRole? {
    if (idLong !in autoRoles.keys) {
        autoRoles[idLong] = mutableMapOf()
    }

    val guildAutoRoles = autoRoles.getOrPut(idLong) { mutableMapOf() }

    if (name !in guildAutoRoles.keys) {
        val autoRole = AutoRole(config, name, id, jda)
        guildAutoRoles[name] = autoRole
        logger.info("Successfully created AutoRole \"$name\" on $id")
        return autoRole
    }
    return guildAutoRoles[name]
}

fun Guild.createAutoRoleIfAbsent(name: String): AutoRole? {
    val config = BotConfiguration.guilds[idLong] ?: return null
    val autoRoleDTO = config.autoRoles[name] ?: return null
    return createAutoRoleIfAbsent(name, autoRoleDTO)
}

fun Guild.loadAutoRoles() {
    val config = BotConfiguration.guilds[idLong] ?: return
    config.autoRoles.forEach { (name, autoRole) ->
        createAutoRoleIfAbsent(name, autoRole)
    }
}

fun clearAutoRoles() {
    autoRoles.values.flatMap { it.values }.forEach {
        it.jda.removeEventListener(it)
    }
    autoRoles.clear()
}
