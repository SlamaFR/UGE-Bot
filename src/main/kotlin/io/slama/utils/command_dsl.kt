package io.slama.utils

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData

operator fun Guild.invoke(dsl: MutableList<CommandData>.() -> Unit) {
    val commands = mutableListOf<CommandData>()
    commands.dsl()
    this.updateCommands().addCommands(commands).queue()
}

operator fun JDA.invoke(dsl: MutableList<CommandData>.() -> Unit) {
    val commands = mutableListOf<CommandData>()
    commands.dsl()
    this.updateCommands().addCommands(commands).queue()
}

fun MutableList<CommandData>.command(
    name: String,
    desc: String,
    options: CommandData.() -> Unit = {}
) {
    val command = CommandData(name, desc)
    command.options()
    this += command
}

fun CommandData.option(
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

fun CommandData.integer(
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
