package io.slama.utils

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

operator fun Guild.invoke(dsl: MutableList<SlashCommandData>.() -> Unit) {
    val commands = mutableListOf<SlashCommandData>()
    commands.dsl()
    this.updateCommands().addCommands(commands).queue()
}

operator fun JDA.invoke(dsl: MutableList<SlashCommandData>.() -> Unit) {
    val commands = mutableListOf<SlashCommandData>()
    commands.dsl()
    this.updateCommands().addCommands(commands).queue()
}

fun MutableList<SlashCommandData>.command(
    name: String,
    desc: String,
    options: SlashCommandData.() -> Unit = {}
) {
    val command = Commands.slash(name, desc)
    command.options()
    this += command
}

fun SlashCommandData.option(
    type: OptionType,
    name: String,
    desc: String,
    required: Boolean = false,
    choices: () -> List<Command.Choice> = { listOf() }
) {
    val option = OptionData(type, name, desc).setRequired(required)
    if (type.canSupportChoices()) option.addChoices(choices())
    this.addOptions(option)
}

fun SlashCommandData.integer(
    name: String,
    desc: String,
    required: Boolean = false,
    minValue: Int? = null,
    maxValue: Int? = null
) {
    val option = OptionData(OptionType.INTEGER, name, desc).setRequired(required)
    if (minValue != null) option.setMinValue(minValue.toLong())
    if (maxValue != null) option.setMaxValue(maxValue.toLong())
    this.addOptions(option)
}
