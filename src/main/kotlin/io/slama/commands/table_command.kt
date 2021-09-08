package io.slama.commands

import io.slama.utils.splitArgs
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*


class TableCommand : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name != "table") return

        val content = event.getOption("content")
        if (content == null) {
            sendUsage(event)
            return
        }

        val args = content.asString.splitArgs()
        val table = ASCIITable()
        for (row in args) {
            table.nextRow()
            for (col in row.split(";")) {
                if (col.isEmpty()) {
                    table.nextCell().blank()
                } else {
                    table.nextCell().setText(col.replace("`", ""))
                }
            }
        }

        if (table.isEmpty) {
            event.reply(":warning: Le tableau est vide !").queue()
        } else {
            val finalTable = table.toString()
            if (finalTable.length > 1990) {
                event.reply(":warning: Le tableau est trop grand !").queue()
            } else {
                event.reply("```\n$finalTable\n```").queue()
            }
        }
    }

    private fun sendUsage(event: SlashCommandEvent) {
        event.replyEmbeds(
            EmbedBuilder().setTitle("Générateur de tableaux ASCII")
                .setDescription(
                    "Cette commande permet de générer rapidement des tableaux avec des caractères ASCII."
                )
                .addField("Utilisation", "`/table <Ligne 1> <Ligne 2> ... <Ligne N>`", false)
                .addField(
                    "Syntaxe", """
                        |Une ligne est divisée en colonnes par le caractère `;`.
                        |Si une ligne contient un ou plusieurs espaces, il faut l'encadrer avec des guillemets pour éviter des comportements inattendus.
                        |Pour dessiner une case vide, sans les bordures, la cellule ne doit contenir que des espaces.
                    """.trimMargin(), false
                )
                .addField(
                    "Exemple 1", """
                        |`/table ";Colonne 1;Colonne 2" "Ligne 1;Val 1;Val 2" ou;comme;ceci`
                        |```
                        |┌─────────┬───────────┬───────────┐
                        |│         │ Colonne 1 │ Colonne 2 │
                        |├─────────┼───────────┼───────────┤
                        |│ Ligne 1 │ Val 1     │ Val 2     │
                        |├─────────┼───────────┼───────────┤
                        |│ ou      │ comme     │ ceci      │
                        |└─────────┴───────────┴───────────┘
                        |```
                    """.trimMargin(), false
                )
                .addField(
                    "Exemple 2", """
                        |`/table " ;Colonne 1;Colonne 2" "Ligne 1;Val 1;Val 2" ou;comme;ceci`
                        |```
                        |          ┌───────────┬───────────┐
                        |          │ Colonne 1 │ Colonne 2 │
                        |┌─────────┼───────────┼───────────┤
                        |│ Ligne 1 │ Val 1     │ Val 2     │
                        |├─────────┼───────────┼───────────┤
                        |│ ou      │ comme     │ ceci      │
                        |└─────────┴───────────┴───────────┘
                        |```
                    """.trimMargin(), false
                )
                .build()
        ).queue()
    }

}

class ASCIITable {

    /**
     * Represents a list of rows, themselves representing a list of cells.
     */
    private val table = mutableListOf<MutableList<String>>()

    /**
     * Represents width of each column.
     */
    private val colWidths = mutableListOf<Int>()

    private var row = 0
    private var col = 0
    private var currentRow = -1
    private var currentCol = -1

    /**
     * @return whether every cell of the table is empty.
     */
    val isEmpty: Boolean
        get() {
            for (row in 0..row) for (col in 0..col) {
                if (!empty(row, col)) {
                    return false
                }
            }
            return true
        }

    /**
     * @return current instance of ASCIITable.
     */
    fun nextRow(): ASCIITable {
        currentRow = addRow() - 1
        currentCol = -1
        return this
    }

    /**
     * @return current instance of ASCIITable.
     */
    fun nextCell(): ASCIITable {
        currentCol++
        if (currentCol == col) {
            currentCol = addColumn() - 1
        }
        return this
    }

    /**
     * Adds an empty cell to the current row.
     *
     * @return current instance of ASCIITable.
     */
    fun blank(): ASCIITable {
        return setText("§")
    }

    /**
     * Adds a new cell to the current row.
     *
     * @param text text to put in the cell.
     * @return current instance of ASCIITable.
     */
    fun setText(text: String): ASCIITable {
        val content = if (" " != text) " $text " else text
        table[currentRow][currentCol] = content
        if (content.length > colWidths[currentCol]) {
            colWidths[currentCol] = content.length - (content.split("\\\\§").dropLastWhile { it.isEmpty() }.size - 1)
        }
        return this
    }

    /**
     * Adds a new row to the bottom, filled with blank cells.
     *
     * @return new row count.
     */
    private fun addRow(): Int {
        table.add(mutableListOf())
        table[row].addAll(Collections.nCopies(col, ""))
        return ++row
    }

    /**
     * Adds a new column to the right, filled with blank cells.
     *
     * @return new column count.
     */
    private fun addColumn(): Int {
        table.forEach { it.add("") }
        colWidths.add(0)
        return ++col
    }

    /**
     * @return whether the cell at [`row`, `col`] is empty.
     */
    private fun empty(row: Int, col: Int): Boolean {
        if (row < 0 || row >= this.row) {
            return true
        }
        if (col < 0 || col >= this.col) {
            return true
        }
        return table[row][col].trim { it <= ' ' }.isEmpty()
    }

    /**
     * @return returns the upper horizontal line of the `n`th row.
     */
    private fun getHorizontalLine(n: Int): String {
        val builder = StringBuilder()
        for (col in 0 until col) {
            builder.append(getIntersect(n, col))
            if (!empty(n - 1, col) || !empty(n, col)) {
                builder.append(HORIZONTAL.toString().repeat(colWidths[col]))
            } else {
                builder.append(' '.toString().repeat(colWidths[col]))
            }
        }
        builder.append(getIntersect(n, col))
        return builder.toString()
    }

    /**
     * @return the char at the intersection of the north west edge of the cell at [`row`, `col`].
     */
    private fun getIntersect(row: Int, col: Int): Char {
        val up = if (empty(row - 1, col - 1) && empty(row - 1, col)) 0 else 1
        val bottom = if (empty(row, col - 1) && empty(row, col)) 0 else 1
        val left = if (empty(row - 1, col - 1) && empty(row, col - 1)) 0 else 1
        val right = if (empty(row - 1, col) && empty(row, col)) 0 else 1
        return CHARS[bottom][up][left][right]
    }

    override fun toString(): String {
        val builder = StringBuilder()
        val r1 = """(?<!\\)§""".toRegex()
        val r2 = """\\§""".toRegex()
        for (row in 0 until row) {
            builder.append(getHorizontalLine(row))
            builder.append('\n')
            for (col in 0 until col) {
                val cellValue = table[row][col].replace(r1, "").replace(r2, "§")
                builder.append(if (!empty(row, col) || !empty(row, col - 1)) VERTICAL else ' ')
                builder.append(cellValue).append(" ".repeat(colWidths[col] - cellValue.length))
            }
            builder.append(if (!empty(row, col - 1)) VERTICAL else ' ')
            builder.append('\n')
        }
        builder.append(getHorizontalLine(row))
        return builder.toString()
    }

    companion object {

        /**
         * Represents chars composing the ASCII table `[bottom][up][left][right]`.
         *
         * Watch out, speaking in Minecraft Enchant Table is mandatory!
         */
        private val CHARS = arrayOf(
            arrayOf(
                arrayOf(charArrayOf(' ', '╶'), charArrayOf('╴', '─')),
                arrayOf(charArrayOf('╵', '└'), charArrayOf('┘', '┴'))
            ),
            arrayOf(
                arrayOf(charArrayOf('╷', '┌'), charArrayOf('┐', '┬')),
                arrayOf(charArrayOf('│', '├'), charArrayOf('┤', '┼'))
            )
        )
        private val HORIZONTAL = CHARS[0][0][1][1]
        private val VERTICAL = CHARS[1][1][0][0]
    }
}
