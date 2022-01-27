package io.slama.core

import io.slama.utils.command
import io.slama.utils.invoke
import io.slama.utils.option
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType

fun JDA.registerGlobalCommands() {
    this {
        command("eval", "Évaluer une expression mathématique.") {
            option(OptionType.STRING, name = "expression", "Expression à évaluer.")
        }
        command("call", "Lancer un appel dans le salon courant.") {
            option(OptionType.INTEGER, name = "timeout", "Temps imparti pour répondre à l'appel en minutes.")
            option(OptionType.ROLE, name = "role", "Groupe d'étudiants visé.")
        }
        command("poll", "Lancer un sondage dans le salon courant.") {
            option(OptionType.STRING, name = "question", "Question sur laquelle porte le sondage.", required = true)
            option(
                OptionType.BOOLEAN,
                name = "log",
                "Sauvegarde un fichier log du résultat du sondage. (Défaut = false)"
            )
            option(
                OptionType.INTEGER,
                name = "timeout",
                "Temps imparti pour répondre au sondage en minutes. (Défaut = 2 minutes)"
            )
            option(OptionType.STRING, name = "a", "Première réponse possible.")
            option(OptionType.STRING, name = "b", "Seconde réponse possible.")
            option(OptionType.STRING, name = "c", "Troisième réponse possible.")
            option(OptionType.STRING, name = "d", "Quatrième réponse possible.")
        }
        command("changen", "Lier un salon à un générateur de salons temporaires.") {
            option(OptionType.CHANNEL, name = "channel", "Salon qui servira de point de départ.", required = true)
        }
        command("table", "Générer un tableau en caractères ASCII.") {
            option(OptionType.STRING, name = "content", "Contenu du tableau. Laisser vide pour obtenir un example.")
        }
    }
}

fun Guild.registerGuildCommands() {
    this {
        command("autorole", "Créer un attributeur automatique de rôle.") {
            option(OptionType.STRING, name = "name", "Nom de l'attributeur.") {
                BotConfiguration.guilds[this@registerGuildCommands.idLong]?.let {
                    it.autoRoles.keys.map { name ->
                        Command.Choice(name, name)
                    }
                } ?: listOf()
            }
        }
    }
}
