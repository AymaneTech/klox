package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.Stmt.Visitor

interface StmtVisitable {
    fun <T> accept(visitor: Visitor<T>): T
}

sealed class Stmt : StmtVisitable {
    data class Block(val statements: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Class(val name: Token, val superClass: Variable?, val methods: List<Function>, val staticMethods: List<Function>): Stmt() {
        override fun <T> accept(visitor: Visitor<T>) : T = visitor.visit(this)
    }

    data class Expression(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Print(val expression: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visit(this)
    }

    data class Return(val token: Token, val value: Expr?) : Stmt() {
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
        fun visit(stmt: Function): T
        fun visit(stmt: While): T
        fun visit(stmt: Return): T
        fun visit(stmt: Class): T
    }
}
