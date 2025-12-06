package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.Expr.Set
import com.aymanetech.Stmt.*
import com.aymanetech.Stmt.Function
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
            when {
                match(CLASS) -> classDeclaration()
                match(FUN) -> function("function")
                match(VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (e: ParserError) {
            synchronize()
            null
        }

    private fun classDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect class name")
        val superClass = if (match(COLON)) {
            consume(IDENTIFIER, "Expect super class name")
            Variable(previous())
        } else null
        consume(LEFT_BRACE, "Expect '{' before class body")

        val methods = mutableListOf<Stmt.Function>()
        val staticMethods = mutableListOf<Stmt.Function>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            if (match(STATIC)) {
                staticMethods.add(function("static method") as Stmt.Function)
            } else {
                methods.add(function("method") as Stmt.Function)
            }
        }
        consume(RIGHT_BRACE, "Expect '}' after class body")
        return Class(name, superClass, methods, staticMethods)
    }

    private fun statement(): Stmt =
        when {
            match(IF) -> ifStatement()
            match(FOR) -> forStatement()
            match(PRINT) -> printStatement()
            match(RETURN) -> returnStatement()
            match(WHILE) -> whileStatement()
            match(LEFT_BRACE) -> block()
            else -> expressionStatement()
        }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'")
        val initializer =
            if (match(SEMICOLON)) null
            else if (match(VAR)) varDeclaration()
            else expressionStatement()

        val condition = if (!check(SEMICOLON)) expression() else Literal(true)
        consume(SEMICOLON, "Expect ';' after loop condition")

        val increment = if (!check(RIGHT_PAREN)) expression() else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses")
        var body = statement()

        if (increment != null) {
            body = Block(
                listOf(
                    body,
                    Expression(increment)
                )
            )
        }

        body = While(condition, body)
        if (initializer != null)
            body = Block(listOf(initializer, body))
        return body
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after if")
        val expression = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return If(expression, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        // NOTE: I skip consuming semicolon to make my language "modern :Joy" inspiration -> tsoding
        skipSemicolon()
        return Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after return value")
        return Return(keyword, value)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initializer = if (match(EQUAL)) expression() else null
        skipSemicolon()
        return Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after while")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition")
        val body = statement()
        return While(condition, body)
    }

    private fun expressionStatement(): Stmt {
        val expression = expression()
        skipSemicolon()
        return Expression(expression)
    }

    private fun function(kind: String): Stmt {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name")
        val parameters: List<Token> = getFunctionParams()
        consume(LEFT_BRACE, "Expect '{' before $kind body")
        val body = blockStatements()
        return Function(name, parameters, body)
    }

    private fun anonymousFunction(): Expr {
        consume(LEFT_PAREN, "Expect '(' after anonymous function")
        val parameters: List<Token> = getFunctionParams()
        consume(LEFT_BRACE, "Expect '{' before function body")
        val body = blockStatements()
        return AnonymousFunction(parameters, body)
    }


    private fun block(): Stmt = Block(blockStatements())

    private fun blockStatements(): List<Stmt> {
        val statements: MutableList<Stmt> = mutableListOf()
        while (!check(RIGHT_BRACE) && !isAtEnd()) statements.add(declaration()!!)
        consume(RIGHT_BRACE, message = "Expect '}' after statement.")
        return statements;
    }

    private fun assignment(): Expr {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            } else if (expr is Get) {
                return Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment expression")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()

        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()

        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Logical(expr, operator, right)
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

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(LEFT_PAREN))
                expr = finishCall(expr)
            else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'")
                expr = Get(expr, name)
            } else
                break
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN))
            do {
                if (arguments.size >= 255)
                    error(peek(), "Can't have more than 255 arguments.")
                arguments.add(expression())
            } while (match(COMMA))
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Call(callee, paren, arguments)
    }

    private fun primary(): Expr =
        when {
            match(FALSE) -> Literal(false)
            match(TRUE) -> Literal(true)
            match(NIL) -> Literal(null)
            match(NUMBER, STRING) -> Literal(previous().literal)
            match(SUPER) -> superKeyword()
            match(THIS) -> This(previous())
            match(IDENTIFIER) -> Variable(previous())
            match(FN) -> anonymousFunction()
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expected ')' after expression")
                Grouping(expr)
            }

            else -> throw error(peek(), "Expect expression.")
        }

    private fun superKeyword(): Expr {
        val keyword = previous()
        consume(DOT, "Expect '.' after keyword")
        val name = consume(IDENTIFIER, "Expect superclass method name")
        return Super(keyword, name)
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

    private fun getFunctionParams(): List<Token> {
        val parameters: MutableList<Token> = mutableListOf()
        if (!check(RIGHT_PAREN))
            do {
                if (parameters.size >= 255)
                    error(peek(), "Can't have more than 255 parameters.")

                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        consume(RIGHT_PAREN, "Expect ')' after parameters.")
        return parameters
    }

    class ParserError : RuntimeException()
}
