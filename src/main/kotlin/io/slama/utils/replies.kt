package io.slama.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import java.awt.Color

fun SlashCommandEvent.replySuccess(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Succès")
            .setDescription(message)
            .setColor(Color(0x2ecc71))
            .build()
    )
}

fun SlashCommandEvent.replyWarning(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Avertissement")
            .setDescription(message)
            .setColor(Color(0xf1c40f))
            .build()
    )
}

fun SlashCommandEvent.replyError(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Erreur")
            .setDescription(message)
            .setColor(Color(0xe74c3c))
            .build()
    )
}
