package com.aymanetech

import com.aymanetech.TokenType.EOF
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.err
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess


object Lox {
    private var hadError = false
    private var hadRuntimeError = false

    private val interpreter = Interpreter()

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
        run(source)
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            print(">>  ")
            val line: String = reader.readLine() ?: break
            run(line)
            hadError = false
        }
    }

    fun run(source: String) {
        val tokens = Scanner(source).scanTokens()
        val statements = Parser(tokens).parse()
        if (hadError) return

        Resolver(interpreter).resolve(statements)
        if (hadError) return
        
        interpreter.interpret(statements)
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
        hadError = true
    }

    fun report(line: Int, where: String, message: String) {
        err.println("[line $line] Error $where: $message")
    }

    fun error(token: Token, message: String) {
        if (token.type == EOF)
            report(token.line, "at end ", message)
        else
            report(token.line, "at '${token.lexeme}'", message)
    }

    fun runtimeError(error: RuntimeError) {
        err.println(
            """
            ${error.message}
            [line ${error.token.line}]
        """.trimIndent()
        )
        hadRuntimeError = true
    }
}