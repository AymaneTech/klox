package com.aymanetech

import com.aymanetech.Lox.error
import com.aymanetech.TokenType.*

class Scanner(val source: String) {
    private val tokens: MutableList<Token> = mutableListOf()
    private val chars: CharArray = source.toCharArray()
    private val keywords: Map<String, TokenType> = mapOf(
        "and" to AND,
        "class" to CLASS,
        "else" to ELSE,
        "false" to FALSE,
        "for" to FOR,
        "fun" to FUN,
        "fn" to FN,
        "if" to IF,
        "nil" to NIL,
        "or" to OR,
        "print" to PRINT,
        "return" to RETURN,
        "super" to SUPER,
        "this" to THIS,
        "true" to TRUE,
        "var" to VAR,
        "while" to WHILE
    )


    private var start: Int = 0
    private var current: Int = 0
    private var line: Int = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)
            '!' -> addToken(takeIfOrElse('=', BANG_EQUAL, BANG))
            '=' -> addToken(takeIfOrElse('=', EQUAL_EQUAL, EQUAL))
            '>' -> addToken(takeIfOrElse('=', GREATER_EQUAL, GREATER))
            '<' -> addToken(takeIfOrElse('=', LESS_EQUAL, LESS))
            '/' ->
                if (match('/')) skipComment()
                else addToken(SLASH)

            ' ', '\r', '\t' -> Unit
            '\n' -> line++
            '"' -> consumeString()
            else ->
                when {
                    isDigit(c) -> consumeNumber()
                    isAlpha(c) -> consumeIdentifier()
                    else -> error(line, message = "Unexpected character '$c'")
                }
        }
    }


    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun advance(): Char = chars[current++]

    private fun peek() =
        if (isAtEnd()) '\u0000' // todo: check that this is equivalent to \0
        else chars[current]

    private fun peekNext() =
        if (current + 1 >= source.length) '\u0000'
        else chars[current + 1]

    private fun consumeString() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }

        if (isAtEnd()) {
            error(line, "unterminated string.")
            return
        }

        advance()

        val value = source.substring(start + 1, current - 1)
        addToken(STRING, value)
    }

    private fun consumeNumber() {
        while (isDigit(peek())) advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()

            while (isDigit(peek())) advance()
        }

        addToken(NUMBER, source.substring(start, current).toDouble())
    }

    private fun consumeIdentifier() {
        while (isAlphaNumeric(peek())) advance()

        val text = source.substring(start, current)
        val type = keywords[text]
        addToken(type ?: IDENTIFIER)
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (chars[current] != expected) return false

        current++
        return true
    }

    private fun isDigit(c: Char) = c in '0'..'9'

    private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

    private fun isAtEnd() = current >= source.length

    private fun skipComment() {
        while (peek() != '\n' && !isAtEnd()) advance()
    }

    private fun takeIfOrElse(char: Char, arg1: TokenType, other: TokenType) =
        if (match(char)) arg1
        else other

}