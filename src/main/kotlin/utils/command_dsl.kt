package utils

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction

operator fun Guild.invoke(dsl: Guild.() -> Unit) {
    this.dsl()
}

operator fun JDA.invoke(dsl: JDA.() -> Unit) {
    this.dsl()
}

fun Guild.command(
    name: String,
    desc: String,
    options: CommandCreateAction.() -> Unit = { }
) {
    this.upsertCommand(name, desc).let { command ->
        command.options()
        command.queue()
    }
}

fun JDA.command(
    name: String,
    desc: String,
    options: CommandCreateAction.() -> Unit = { }
) {
    this.upsertCommand(name, desc).let { command ->
        command.options()
        command.queue()
    }
}

fun CommandCreateAction.option(
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