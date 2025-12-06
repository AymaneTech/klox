package com.aymanetech.resolver

import com.aymanetech.interpreter.Interpreter
import com.aymanetech.runtime.errors.ErrorHandler
import com.aymanetech.ast.Expr
import com.aymanetech.ast.Stmt
import com.aymanetech.lexer.Token
import java.util.Stack
import kotlin.collections.forEach

class Resolver(
    private val interpreter: Interpreter,
    private val errorHandler: ErrorHandler
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    override fun visit(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visit(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visit(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visit(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visit(expr: Expr.Variable) {
        if (scopes.isNotEmpty() && scopes.peek()[expr.name.lexeme] == false)
            errorHandler.reportError(expr.name, "Can't read local variable in it's own initializer")

        resolveLocal(expr, expr.name)
    }

    override fun visit(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visit(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visit(expr: Expr.Super) {
        if(currentClass == ClassType.NONE)
            errorHandler.reportError(expr.keyword, "Can't use 'super' outside of a class.")
        else if (currentClass != ClassType.SUBCLASS)
            errorHandler.reportError(expr.keyword, "Can't use 'super' in a class without a superclass.")

        resolveLocal(expr, expr.keyword)
    }

    override fun visit(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            errorHandler.reportError(expr.keyword, "Can't use 'this' outside of a method")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visit(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments?.forEach(::resolve)
    }

    override fun visit(expr: Expr.AnonymousFunction) {
        val enclosingFunction = currentFunction
        currentFunction = FunctionType.FUNCTION
        beginScope()
        expr.params.forEach {
            declare(it)
            define(it)
        }
        resolve(expr.body)
        endScope()
        currentFunction = enclosingFunction
    }

    override fun visit(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visit(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visit(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visit(expr: Expr.Literal) {
        // Literally nothing :joy
    }

    override fun visit(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visit(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visit(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visit(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE)
            errorHandler.reportError(stmt.token, "Can't return from top-level code")

        if (stmt.value != null && currentFunction == FunctionType.INITIALIZER)
            errorHandler.reportError(stmt.token, "Can't return from a constructor")

        stmt.value?.let { resolve(it) }
    }

    override fun visit(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS
        declare(stmt.name)
        define(stmt.name)
        if(stmt.superClass != null && stmt.name.lexeme.equals(stmt.superClass.name.lexeme))
            errorHandler.reportError(stmt.superClass.name, "A class can't inherit from itself")

        if (stmt.superClass != null){
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superClass)
        }

        if(stmt.superClass != null){
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek()["this"] = true
        stmt.methods.forEach {
            val declaration = if (it.name.lexeme != "init") FunctionType.METHOD else FunctionType.INITIALIZER
            resolveFunction(it, declaration)
        }
        endScope()
        if(stmt.superClass != null) endScope()
        currentClass = enclosingClass
    }

    override fun visit(expr: Expr.Get) {
        resolve(expr.obj)
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
            errorHandler.reportError(name, "Already variable with this name in this scope")
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

    enum class FunctionType {
        NONE, FUNCTION, METHOD, INITIALIZER
    }

    enum class ClassType {
        NONE, CLASS, SUBCLASS
    }
}