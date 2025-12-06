package com.aymanetech.parser

import com.aymanetech.Lox
import com.aymanetech.ast.Expr
import com.aymanetech.ast.Stmt
import com.aymanetech.lexer.Token
import com.aymanetech.lexer.TokenType

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
            when {
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.FUN) -> function("function")
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (e: ParserError) {
            synchronize()
            null
        }

    private fun classDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect class name")
        val superClass = if (match(TokenType.COLON)) {
            consume(TokenType.IDENTIFIER, "Expect super class name")
            Expr.Variable(previous())
        } else null
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body")

        val methods = mutableListOf<Stmt.Function>()
        val staticMethods = mutableListOf<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.STATIC)) {
                staticMethods.add(function("static method") as Stmt.Function)
            } else {
                methods.add(function("method") as Stmt.Function)
            }
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body")
        return Stmt.Class(name, superClass, methods, staticMethods)
    }

    private fun statement(): Stmt =
        when {
            match(TokenType.IF) -> ifStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.LEFT_BRACE) -> block()
            else -> expressionStatement()
        }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'")
        val initializer =
            if (match(TokenType.SEMICOLON)) null
            else if (match(TokenType.VAR)) varDeclaration()
            else expressionStatement()

        val condition = if (!check(TokenType.SEMICOLON)) expression() else Expr.Literal(true)
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition")

        val increment = if (!check(TokenType.RIGHT_PAREN)) expression() else null
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses")
        var body = statement()

        if (increment != null) {
            body = Stmt.Block(
                listOf(
                    body,
                    Stmt.Expression(increment)
                )
            )
        }

        body = Stmt.While(condition, body)
        if (initializer != null)
            body = Stmt.Block(listOf(initializer, body))
        return body
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if")
        val expression = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition")

        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) statement() else null

        return Stmt.If(expression, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        // NOTE: I skip consuming semicolon to make my language "modern :Joy" inspiration -> tsoding
        skipSemicolon()
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON)) expression() else null
        consume(TokenType.SEMICOLON, "Expect ';' after return value")
        return Stmt.Return(keyword, value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        val initializer = if (match(TokenType.EQUAL)) expression() else null
        skipSemicolon()
        return Stmt.Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after while")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expression = expression()
        skipSemicolon()
        return Stmt.Expression(expression)
    }

    private fun function(kind: String): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name")
        val parameters: List<Token> = getFunctionParams()
        consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body")
        val body = blockStatements()
        return Stmt.Function(name, parameters, body)
    }

    private fun anonymousFunction(): Expr {
        consume(TokenType.LEFT_PAREN, "Expect '(' after anonymous function")
        val parameters: List<Token> = getFunctionParams()
        consume(TokenType.LEFT_BRACE, "Expect '{' before function body")
        val body = blockStatements()
        return Expr.AnonymousFunction(parameters, body)
    }


    private fun block(): Stmt = Stmt.Block(blockStatements())

    private fun blockStatements(): List<Stmt> {
        val statements: MutableList<Stmt> = mutableListOf()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) statements.add(declaration()!!)
        consume(TokenType.RIGHT_BRACE, message = "Expect '}' after statement.")
        return statements;
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment expression")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr: Expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN))
                expr = finishCall(expr)
            else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'")
                expr = Expr.Get(expr, name)
            } else
                break
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN))
            do {
                if (arguments.size >= 255)
                    error(peek(), "Can't have more than 255 arguments.")
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr =
        when {
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
            match(TokenType.SUPER) -> superKeyword()
            match(TokenType.THIS) -> Expr.This(previous())
            match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
            match(TokenType.FN) -> anonymousFunction()
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
                Expr.Grouping(expr)
            }

            else -> throw error(peek(), "Expect expression.")
        }

    private fun superKeyword(): Expr {
        val keyword = previous()
        consume(TokenType.DOT, "Expect '.' after keyword")
        val name = consume(TokenType.IDENTIFIER, "Expect superclass method name")
        return Expr.Super(keyword, name)
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
        Lox.error(token, message)
        return ParserError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return

            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                else -> {}
            }
            advance()
        }
    }

    private fun isAtEnd() = peek().type == TokenType.EOF

    private fun peek() = tokens[current]

    private fun previous() = tokens[current - 1]

    private fun skipSemicolon() {
        match(TokenType.SEMICOLON)
    }

    private fun getFunctionParams(): List<Token> {
        val parameters: MutableList<Token> = mutableListOf()
        if (!check(TokenType.RIGHT_PAREN))
            do {
                if (parameters.size >= 255)
                    error(peek(), "Can't have more than 255 parameters.")

                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
        return parameters
    }

    class ParserError : RuntimeException()
}