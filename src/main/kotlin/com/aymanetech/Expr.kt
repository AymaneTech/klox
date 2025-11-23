package com.aymanetech

import com.aymanetech.Expr.Visitor

interface ExprVisitable {
    fun <T> accept(visitor: Visitor<T>): T
}

sealed class Expr : ExprVisitable {

    data class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Grouping(val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Literal(val value: Any?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
    }

    data class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Variable(val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    interface Visitor<T> {
        fun visit(expr: Assign): T
        fun visit(expr: Binary): T
        fun visit(expr: Grouping): T
        fun visit(expr: Literal): T
        fun visit(expr: Unary): T
        fun visit(expr: Variable): T
        fun visit(expr: Logical): T
        fun visit(expr: Call): T
    }
}

//class AstPrinter : Visitor<String> {
//
//    fun print(expression: Expr): String = expression.accept(this);
//
//    override fun visit(expr: Binary): String = parenthesize(expr.operator.lexeme, expr.left, expr.right)
//
//    override fun visit(expr: Grouping): String = parenthesize("group", expr.expression)
//
//    override fun visit(expr: Literal): String =
//        if (expr.value == null) "nil"
//        else expr.value.toString()
//
//    override fun visit(expr: Unary): String = parenthesize(expr.operator.lexeme, expr.right)
//
//    override fun visit(expr: Variable): String = expr.accept(this)
//
//    override fun visit(expr: Assign): String = expr.value.accept(this)
//
//    private fun parenthesize(name: String, vararg expressions: Expr): String =
//        buildString {
//            append("(")
//            append(name)
//            expressions.forEach {
//                append(" ")
//                append(it.accept(this@AstPrinter))
//            }
//            append(")")
//        }
//}