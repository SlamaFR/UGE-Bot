package io.slama.commands

import io.slama.events.createAutoRoleIfAbsent
import io.slama.utils.replyError
import io.slama.utils.replySuccess
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class AutoRoleCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "autorole") return
        if (event.guild == null) return

        val name = event.getOption("name") ?: return
        val autoRole = event.guild?.run { createAutoRoleIfAbsent(name.asString) }

        if (autoRole != null) {
            event.replySuccess("L'attributeur a été créé avec succès !")
                .setEphemeral(true)
                .queue()
            autoRole.send(event.messageChannel)
        } else {
            event.replyError("L'attributeur `${name.asString}` n'existe pas. Contactez votre administrateur si vous pensez qu'il s'agit d'une erreur.")
                .setEphemeral(true)
                .queue()
        }
    }
}
