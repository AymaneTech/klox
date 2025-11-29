package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.FunctionType.FUNCTION
import com.aymanetech.FunctionType.NONE
import com.aymanetech.Lox.error
import com.aymanetech.Stmt.*
import java.util.*

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = NONE

    override fun visit(stmt: Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visit(stmt: If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visit(stmt: Expression) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Print) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visit(expr: Variable) {
        if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false)
            error(expr.name, "Can't read local variable in it's own initializer")

        resolveLocal(expr, expr.name)
    }

    override fun visit(expr: Logical) {
        resolve(expr.left)
        resolve(expr.left)
    }

    override fun visit(expr: Call) {
        resolve(expr.callee)
        expr.arguments?.forEach(::resolve)
    }

    override fun visit(expr: AnonymousFunction) {
        val enclosingFunction = currentFunction
        currentFunction = FUNCTION
        beginScope()
        expr.params.forEach {
            declare(it)
            define(it)
        }
        resolve(expr.body)
        endScope()
        currentFunction = enclosingFunction
    }

    override fun visit(expr: Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visit(expr: Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visit(expr: Grouping) {
        resolve(expr.expression)
    }

    override fun visit(expr: Literal) {
        // Literally nothing :joy
    }

    override fun visit(expr: Unary) {
        resolve(expr.right)
    }

    override fun visit(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FUNCTION)
    }

    override fun visit(stmt: While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visit(stmt: Return) {
        if (currentFunction == NONE)
            error(stmt.token, "Can't return from top-level code")

        stmt.value?.let { resolve(it) }
    }

    fun resolve(statements: List<Stmt>) {
        statements.forEach(::resolve)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolveFunction(stmt: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        stmt.params.forEach {
            declare(it)
            define(it)
        }
        resolve(stmt.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme))
            error(name, "Already variable with this name in this scope")
        scope[name.lexeme] = false

    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) return
        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.lastIndex downTo 0) {
            if (name.lexeme in scopes[i]) {
                interpreter.resolve(expr, scopes.lastIndex - i)
                return
            }
        }
    }
}

enum class FunctionType {
    NONE, FUNCTION
}

