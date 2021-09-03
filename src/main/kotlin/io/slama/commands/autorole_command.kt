package io.slama.commands

import io.slama.events.createAutoRoleIfAbsent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class AutoRoleCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "autorole") return
        if (event.guild == null) return

        val name = event.getOption("name") ?: return
        val autoRole = event.guild?.let { createAutoRoleIfAbsent(it, name.asString) }

        if (autoRole != null) {
            event.reply(":white_check_mark: L'attributeur a été créé avec succès !")
                .setEphemeral(true)
                .queue()
            autoRole.send(event.textChannel)
        } else {
            event.reply(
                "**Erreur :** l'attributeur `${name.asString}` n'existe pas. " +
                        "Contactez votre administrateur si vous pensez qu'il s'agit d'une erreur."
            )
                .setEphemeral(true)
                .queue()
        }
    }

}