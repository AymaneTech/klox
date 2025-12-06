package com.aymanetech

import com.aymanetech.interpreter.Interpreter
import com.aymanetech.runtime.errors.ErrorHandler
import com.aymanetech.lexer.Scanner
import com.aymanetech.parser.Parser
import com.aymanetech.resolver.Resolver
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


object Lox {
    private val interpreter = Interpreter()
    private val errorHandler = ErrorHandler()
    private val terminal = Terminal()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size > 1) {
            println("Usage: klox [script]")
            exitProcess(64)
        } else if (args.size == 1) {
            runFile(args[0])
        } else {
            runPrompt()
        }
    }

    fun runFile(path: String) {
        val bytes = Files.readAllBytes(Paths.get(path))
        val source = String(bytes, Charset.defaultCharset())
        errorHandler.setSource(source, path)
        run(source)
        if (errorHandler.hadError) exitProcess(65)
        if (errorHandler.hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        terminal.println(cyan(bold("\n╔════════════════════════════════════════╗")))
        terminal.println(cyan(bold("║")) + "   " + magenta(bold("Welcome to KLox REPL")) + "           " + cyan(bold("║")))
        terminal.println(cyan(bold("║")) + "   " + dim("Type 'exit' or Ctrl+D to quit") + "   " + cyan(bold("║")))
        terminal.println(cyan(bold("╚════════════════════════════════════════╝\n")))

        while (true) {
            terminal.print(green(bold("klox> ")))
            val line: String = reader.readLine() ?: break

            if (line.trim() == "exit") break
            if (line.trim().isEmpty()) continue

            errorHandler.setSource(line, "repl")
            run(line)
            errorHandler.reset()
        }

        terminal.println(cyan("\n👋 Goodbye!"))
    }

    fun run(source: String) {
        val tokens = Scanner(source, errorHandler).scanTokens()
        val statements = Parser(tokens, errorHandler).parse()
        if (errorHandler.hadError) return

        Resolver(interpreter, errorHandler).resolve(statements)
        if (errorHandler.hadError) return

        interpreter.interpret(statements, errorHandler)
    }
}