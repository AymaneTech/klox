package com.aymanetech.runtime.errors

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal

class DiagnosticReporter(
    private val source: String,
    private val filename: String = "script.lox"
) {
    private val terminal = Terminal()
    private val lines = source.lines()

    fun reportError(
        line: Int,
        column: Int = 0,
        length: Int = 1,
        message: String,
        hint: String? = null,
        where: String = ""
    ) {
        if (line < 1 || line > lines.size) {
            terminal.println(red(bold("error")) + ": $message")
            terminal.println(blue("  --> $filename:$line"))
            terminal.println()
            return
        }

        val lineNumber = line.toString().padStart(4)
        val errorLine = lines[line - 1]

        terminal.println(red(bold("error")) + ": $message")
        terminal.println(blue("  --> $filename:$line:$column"))
        terminal.println(blue("   |"))

        if (line > 1 && lines.size > 1) {
            val prevLineNum = (line - 1).toString().padStart(4)
            terminal.println(blue(" $prevLineNum | ") + dim(lines[line - 2]))
        }

        terminal.println(blue(" $lineNumber | ") + errorLine)

        if (column > 0) {
            val padding = " ".repeat(column - 1)
            val underline = "^".repeat(length.coerceAtLeast(1))
            terminal.println(blue("      | ") + red(bold(padding + underline)))
            if (where.isNotEmpty()) {
                terminal.println(blue("      | ") + red(padding + where))
            }
        }

        if (hint != null) {
            terminal.println(blue("      = ") + cyan(bold("hint: ") + hint))
        }

        if (line < lines.size) {
            val nextLineNum = (line + 1).toString().padStart(4)
            terminal.println(blue(" $nextLineNum | ") + dim(lines[line]))
        }

        terminal.println(blue("   |"))
        terminal.println()
    }

    fun reportRuntimeError(
        line: Int,
        message: String,
        hint: String? = null
    ) {
        if (line < 1 || line > lines.size) {
            terminal.println(red(bold("runtime error")) + ": $message")
            terminal.println(blue("  --> $filename:$line"))
            terminal.println()
            return
        }

        val lineNumber = line.toString().padStart(4)
        val errorLine = lines[line - 1]

        terminal.println(red(bold("runtime error")) + ": $message")
        terminal.println(blue("  --> $filename:$line"))
        terminal.println(blue("   |"))

        if (line > 1 && lines.size > 1) {
            val prevLineNum = (line - 1).toString().padStart(4)
            terminal.println(blue(" $prevLineNum | ") + dim(lines[line - 2]))
        }

        terminal.println(blue(" $lineNumber | ") + errorLine)

        if (hint != null) {
            terminal.println(blue("      = ") + cyan(bold("hint: ") + hint))
        }

        if (line < lines.size) {
            val nextLineNum = (line + 1).toString().padStart(4)
            terminal.println(blue(" $nextLineNum | ") + dim(lines[line]))
        }

        terminal.println(blue("   |"))
        terminal.println()
    }
}
