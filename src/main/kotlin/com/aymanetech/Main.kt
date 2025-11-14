package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.TokenType.MINUS
import com.aymanetech.TokenType.STAR
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.err
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private var hadError = false

fun main(args: Array<String>) {
//    if (args.size > 1) {
//        println("Usage: klox [script]")
//        exitProcess(64)
//    } else if (args.size == 1) {
//        runFile(args[0])
//    } else {
//        runPrompt()
//    }
    val expression = Binary(
        left = Unary(
            Token(MINUS, "-", null, 1),
            Literal(123),
        ),
        operator = Token(STAR, "*", null, 1),
        right = Grouping(Literal(45.67))
    )

    println(AstPrinter().print(expression))
}

fun runFile(path: String) {
    println("Run file: $path")
    val bytes = Files.readAllBytes(Paths.get(path))
    val source = String(bytes, Charset.defaultCharset())
    run(source)
    if (hadError) exitProcess(65)
}

fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        val line: String = reader.readLine() ?: break
        run(line)

        hadError = false
    }
}

fun run(source: String) {
    println("source: $source")
    val tokens = Scanner(source)
        .scanTokens()

    tokens.forEach { println(it) }
}

fun error(line: Int, message: String) {
    report(line, "", message)
    hadError = true
}

fun report(line: Int, where: String, message: String) {
    err.println("[line $line] Error $where: $message")
}

