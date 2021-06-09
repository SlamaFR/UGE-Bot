package utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction

operator fun Guild.invoke(dsl: Guild.() -> Unit) {
    this.dsl()
}

fun Guild.command(name: String, desc: String, options: CommandCreateAction.() -> Unit = { }) {
    this.upsertCommand(name, desc).let { command ->
        command.options()
        command.queue()
    }
}


fun CommandCreateAction.option(type: OptionType, name: String, desc: String, required: Boolean = false) {
    this.addOption(type, name, desc, required)
}
