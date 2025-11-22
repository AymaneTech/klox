package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.Stmt.*
import com.aymanetech.TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!isAtEnd()) statements.add(declaration())

        return statements.filterNotNull().toList()
    }

    private fun expression(): Expr = assignment()

    private fun declaration(): Stmt? =
        try {
            if (match(VAR)) varDeclaration()
            else statement()
        } catch (e: ParserError) {
            synchronize()
            null
        }

    private fun statement(): Stmt {
        return if (match(PRINT)) printStatement()
        else if (match(LEFT_BRACE)) block()
        else expressionStatement()
    }

    private fun printStatement(): Stmt {
        val value = expression()
        // NOTE: I skip consuming semicolon to make my language "moder :Joy"
        skipSemicolon()
        return Print(value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initializer = if (match(EQUAL)) expression() else null
        skipSemicolon()
        return Var(name, initializer)
    }

    private fun expressionStatement(): Stmt {
        val expression = expression()
        skipSemicolon()
        return Expression(expression)
    }

    private fun block(): Stmt = Block(blockStatements())

    private fun blockStatements(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!check(RIGHT_BRACE) && !isAtEnd()) statements.add(declaration()!!)
        consume(RIGHT_BRACE, message = "Expect '}' after statement.")
        return statements;
    }

    private fun assignment(): Expr {
        val expr = equality()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            }
            error(equals, "Invalid assignment expression")
        }
        return expr
    }

    private fun equality(): Expr {
        var expr: Expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = comparison()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr =
        when {
            match(FALSE) -> Literal(false)
            match(TRUE) -> Literal(true)
            match(NIL) -> Literal(null)
            match(NUMBER, STRING) -> Literal(previous().literal)
            match(IDENTIFIER) -> Variable(previous())
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expected ')' after expression")
                Grouping(expr)
            }

            else -> throw error(peek(), "Expect expression.")
        }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun error(token: Token, message: String): ParserError {
        Lexer.error(token, message)
        return ParserError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return

            when (peek().type) {
                CLASS, FUN, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> {}
            }
            advance()
        }
    }

    private fun isAtEnd() = peek().type == EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun skipSemicolon() {
        match(SEMICOLON)
    }

    class ParserError : RuntimeException()
}
