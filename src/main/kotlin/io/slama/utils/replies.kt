package io.slama.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import java.awt.Color

object EmbedColors {
    val PURPLE = Color(0x5913E0)
    val VIOLET = Color(0x9B59B6)
    val BLUE = Color(0x09b6c2)
    val GREEN = Color(0x2ecc71)
    val YELLOW = Color(0xf1c40f)
    val ORANGE = Color(0xe67e22)
    val RED = Color(0xe74c3c)
}

private fun successEmbed(message: String): MessageEmbed = EmbedBuilder()
    .setTitle("Succès")
    .setDescription(message)
    .setColor(EmbedColors.GREEN)
    .build()

private fun warningEmbed(message: String): MessageEmbed = EmbedBuilder()
    .setTitle("Avertissement")
    .setDescription(message)
    .setColor(EmbedColors.YELLOW)
    .build()

private fun errorEmbed(message: String): MessageEmbed = EmbedBuilder()
    .setTitle("Erreur")
    .setDescription(message)
    .setColor(EmbedColors.RED)
    .build()

fun SlashCommandEvent.replySuccess(message: String): ReplyAction =
    this.replyEmbeds(successEmbed(message))

fun ButtonClickEvent.replySuccess(message: String): ReplyAction =
    this.replyEmbeds(successEmbed(message))

fun MessageChannel.sendSuccess(message: String): MessageAction =
    this.sendMessage(successEmbed(message))

fun SlashCommandEvent.replyWarning(message: String): ReplyAction =
    this.replyEmbeds(warningEmbed(message))

fun ButtonClickEvent.replyWarning(message: String): ReplyAction =
    this.replyEmbeds(warningEmbed(message))

fun MessageChannel.sendWarning(message: String): MessageAction =
    this.sendMessage(warningEmbed(message))

fun SlashCommandEvent.replyError(message: String): ReplyAction =
    this.replyEmbeds(errorEmbed(message))

fun ButtonClickEvent.replyError(message: String): ReplyAction =
    this.replyEmbeds(errorEmbed(message))

fun MessageChannel.sendError(message: String): MessageAction =
    this.sendMessage(errorEmbed(message))
