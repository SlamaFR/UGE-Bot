package io.slama.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import java.awt.Color

val GREEN = Color(0x2ecc71)
val YELLOW = Color(0xf1c40f)
val RED = Color(0xe74c3c)

fun SlashCommandEvent.replySuccess(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Succès")
            .setDescription(message)
            .setColor(GREEN)
            .build()
    )
}

fun MessageChannel.sendSuccess(message: String): MessageAction {
    return this.sendMessage(
        EmbedBuilder()
            .setTitle("Succès")
            .setDescription(message)
            .setColor(GREEN)
            .build()
    )
}

fun SlashCommandEvent.replyWarning(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Avertissement")
            .setDescription(message)
            .setColor(YELLOW)
            .build()
    )
}

fun MessageChannel.sendWarning(message: String): MessageAction {
    return this.sendMessage(
        EmbedBuilder()
            .setTitle("Avertissement")
            .setDescription(message)
            .setColor(YELLOW)
            .build()
    )
}

fun SlashCommandEvent.replyError(message: String): ReplyAction {
    return this.replyEmbeds(
        EmbedBuilder()
            .setTitle("Erreur")
            .setDescription(message)
            .setColor(RED)
            .build()
    )
}

fun MessageChannel.sendError(message: String): MessageAction {
    return this.sendMessage(
        EmbedBuilder()
            .setTitle("Erreur")
            .setDescription(message)
            .setColor(RED)
            .build()
    )
}
