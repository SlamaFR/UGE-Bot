package io.slama.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.awt.Color

object EmbedColors {
    val PURPLE = Color(0x8e44ad)
    val VIOLET = Color(0x9b59b6)
    val BLUE = Color(0x3498db)
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

fun SlashCommandInteractionEvent.replySuccess(message: String): ReplyCallbackAction =
    this.replyEmbeds(successEmbed(message))

fun ButtonInteractionEvent.replySuccess(message: String): ReplyCallbackAction =
    this.replyEmbeds(successEmbed(message))

fun MessageChannel.sendSuccess(message: String): MessageCreateAction =
    this.sendMessageEmbeds(successEmbed(message))

fun SlashCommandInteractionEvent.replyWarning(message: String): ReplyCallbackAction =
    this.replyEmbeds(warningEmbed(message))

fun ButtonInteractionEvent.replyWarning(message: String): ReplyCallbackAction =
    this.replyEmbeds(warningEmbed(message))

fun MessageChannel.sendWarning(message: String): MessageCreateAction =
    this.sendMessageEmbeds(warningEmbed(message))

fun SlashCommandInteractionEvent.replyError(message: String): ReplyCallbackAction =
    this.replyEmbeds(errorEmbed(message))

fun ButtonInteractionEvent.replyError(message: String): ReplyCallbackAction =
    this.replyEmbeds(errorEmbed(message))

fun MessageChannel.sendError(message: String): MessageCreateAction =
    this.sendMessageEmbeds(errorEmbed(message))
