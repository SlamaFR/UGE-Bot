package core

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import utils.*

fun JDA.registerGlobalCommands() {
    this {
        command("keval", "Évaluer une expression mathématique.") {
            option(OptionType.STRING, name = "expression", "Expression à évaluer.")
        }
        command("call", "Lancer un appel dans le salon courant.") {
            option(OptionType.INTEGER, name = "timeout", "Temps imparti pour répondre à l'appel en minutes.")
            option(OptionType.ROLE, name = "role", "Groupe d'étudiants visé.")
        }
    }
}

fun Guild.registerGuildCommands() {
    this {
        command("autorole", "Créer un attributeur automatique de rôle.") {
            option(OptionType.STRING, name = "name", "Nom de l'attributeur.") {
                this@registerGuildCommands.getConfigOrNull()?.let {
                    it.autoRoles.keys.map { name ->
                        Command.Choice(name, name)
                    }
                } ?: listOf()
            }
        }
    }
}
