package io.slama.commands

import com.notkamui.keval.Keval
import io.slama.utils.EmbedColors
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class KevalCommand : ListenerAdapter() {
    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "eval") return
        if (event.guild == null) return

        val expr = event.getOption("expression")
        if (expr == null) {
            event.replyEmbeds(helpEmbed).queue()
            return
        }

        val res = keval.eval(expr.asString)
        event.replyEmbeds(EmbedBuilder()
            .setTitle("Eval")
            .addField("Expression", expr.asString, false)
            .addField("Résultat", res.toString(), false)
            .setFooter("Powered by Keval.")
            .setColor(EmbedColors.BLUE)
            .build())
            .queue()
    }
}

private val helpEmbed = EmbedBuilder()
    .setTitle("Eval")
    .setDescription("Help")
    .addField(
        "Opérateurs binaires",
        """
        - Soustraction `-`
        - Addition `+`
        - Multiplication `*`
        - Division `/`
        - Puissance `^`
        - Modulo `%`
        """.trimIndent(),
        false
    )
    .addField(
        "Fonctions",
        """
        - Opposé `neg(expr)`
        - Maximum `max(a, b)`
        - Minimum `min(a, b)`
        - Racine carrée `sqrt(expr)`
        - Sinus `sin(expr)`
        - Cosinus `cos(expr)`
        - Tangente `tan(expr)`
        - Arcsinus `asin(expr)`
        - Arccosinus `acos(expr)`
        - Arctangente `atan(expr)`
        - Random `rand()` ([0; 1[)
        - Arrondi inférieur `floor(expr)`
        - Arrondi supérieur `ceil(expr)`
        - Logarithme base 10 `log(expr)`
        - Logarithme base 2 `logB(expr)`
        - Logarithme népérien `ln(expr)`
        """.trimIndent(),
        false
    )
    .addField(
        "Constantes",
        """
        - π `PI`
        - *e* `e` (constante de Néper)
        - φ `PHI`
        """.trimIndent(),
        false
    )
    .setFooter("N'hésitez pas à proposer de nouvelles fonctions (avec nom et arité). Powered by Keval.")
    .setColor(EmbedColors.BLUE)
    .build()

private val keval = Keval {
    includeDefault()

    constant {
        name = "PHI"
        value = (1 + sqrt(5.0)) / 2
    }

    function {
        name = "max"
        arity = 2
        implementation = { max(it[0], it[1]) }
    }
    function {
        name = "min"
        arity = 2
        implementation = { min(it[0], it[1]) }
    }
    function {
        name = "sqrt"
        arity = 1
        implementation = { sqrt(it[0]) }
    }
    function {
        name = "sin"
        arity = 1
        implementation = { sin(it[0]) }
    }
    function {
        name = "cos"
        arity = 1
        implementation = { cos(it[0]) }
    }
    function {
        name = "tan"
        arity = 1
        implementation = { tan(it[0]) }
    }
    function {
        name = "asin"
        arity = 1
        implementation = { asin(it[0]) }
    }
    function {
        name = "acos"
        arity = 1
        implementation = { acos(it[0]) }
    }
    function {
        name = "atan"
        arity = 1
        implementation = { atan(it[0]) }
    }
    function {
        name = "rand"
        arity = 0
        implementation = { Math.random() }
    }
    function {
        name = "floor"
        arity = 1
        implementation = { floor(it[0]) }
    }
    function {
        name = "ceil"
        arity = 1
        implementation = { ceil(it[0]) }
    }
    function {
        name = "log"
        arity = 1
        implementation = { log(it[0], 10.0) }
    }
    function {
        name = "logB"
        arity = 1
        implementation = { log(it[0], 2.0) }
    }
    function {
        name = "ln"
        arity = 1
        implementation = { ln(it[0]) }
    }
}