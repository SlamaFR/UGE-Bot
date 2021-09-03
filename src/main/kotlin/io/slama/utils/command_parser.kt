package io.slama.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.io.File

@Serializable
data class CommandGroupDTO(
    val commands: List<CommandDTO>
)

@Serializable
data class CommandDTO(
    val name: String,
    val description: String,
    val options: List<OptionDTO>,
)

@Serializable
data class OptionDTO(
    val name: String,
    val description: String,
    val type: OptionType,
    val required: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@Deprecated("Moved to DSL")
fun Guild.addCommands() {
    val json = File("config/commands.json").readText()
    val group = Json.decodeFromString<CommandGroupDTO>(json)
    group.commands.forEach { command ->
        this.upsertCommand(command.name, command.description).apply {
            command.options.forEach { option ->
                addOption(option.type, option.name, option.description, option.required)
            }
        }.queue()
    }
}
