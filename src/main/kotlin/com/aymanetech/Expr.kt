package com.aymanetech

import com.aymanetech.Expr.*

sealed class Expr {
    abstract fun <T> accept(visitor: Visitor<T>): T

    data class Binary(var left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitBinary(this)
    }

    data class Grouping(val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitGrouping(this)
    }

    data class Literal(val value: Any?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitLiteral(this)

    }

    data class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T =
            visitor.visitUnary(this)
    }
}

interface Visitor<T> {
    fun visitBinary(expr: Binary): T
    fun visitGrouping(expr: Grouping): T
    fun visitLiteral(expr: Literal): T
    fun visitUnary(expr: Unary): T
}

class AstPrinter : Visitor<String> {

    fun print(expression: Expr): String =
        expression.accept(this);


    override fun visitBinary(expr: Binary): String =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)


    override fun visitGrouping(expr: Grouping): String =
        parenthesize("group", expr.expression)


    override fun visitLiteral(expr: Literal): String =
        if (expr.value == null) "nil"
        else expr.value.toString()

    override fun visitUnary(expr: Unary): String =
        parenthesize(expr.operator.lexeme, expr.right)

    private fun parenthesize(name: String, vararg expressions: Expr): String =
        buildString {
            append("(")
            append(name)
            expressions.forEach {
                append(" ")
                append(it.accept(this@AstPrinter))
            }
            append(")")
        }
}