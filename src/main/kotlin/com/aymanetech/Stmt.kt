package com.aymanetech

import com.aymanetech.Stmt.Visitor

interface StmtVisitable {
    fun <T> accept(visitor: Visitor<T>): T
}

sealed class Stmt : StmtVisitable {
    data class Block(val statements: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Expression(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Print(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Var(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    interface Visitor<T> {
        fun visit(stmt: Expression): T
        fun visit(stmt: Print): T
        fun visit(stmt: Var): T
        fun visit(stmt: Block): T
        fun visit(stmt: If): T
        fun visit(stmt: While): T
    }
}
