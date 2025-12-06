package com.aymanetech.ast

import com.aymanetech.ast.Expr.Visitor
import com.aymanetech.lexer.Token

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

    data class Get(val obj: Expr, val name: Token) : Expr() {
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

    data class Set(val obj: Expr, val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
    }

    data class Super(val keyword: Token, val method: Token): Expr() {
        override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
    }

    data class This(val keyword: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
    }

    data class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Variable(val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class AnonymousFunction(val params: List<Token>, val body: List<Stmt>) : Expr(){
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
        fun visit(expr: AnonymousFunction): T
        fun visit(expr: Get): T
        fun visit(expr: Set): T
        fun visit(expr: This): T
        fun visit(expr: Super): T
    }
}