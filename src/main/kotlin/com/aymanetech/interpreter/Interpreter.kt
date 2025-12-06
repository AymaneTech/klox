package com.aymanetech.interpreter

import com.aymanetech.runtime.errors.ErrorHandler
import com.aymanetech.ast.Expr
import com.aymanetech.ast.Expr.*
import com.aymanetech.ast.Stmt
import com.aymanetech.ast.Stmt.*
import com.aymanetech.lexer.Token
import com.aymanetech.lexer.TokenType.*
import com.aymanetech.runtime.LoxCallable
import com.aymanetech.runtime.LoxClass
import com.aymanetech.runtime.LoxFunction
import com.aymanetech.runtime.LoxInstance
import com.aymanetech.runtime.errors.RuntimeError
import com.aymanetech.runtime.errors.RuntimeReturn

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private val globals: Environment = Environment()
    private var environment: Environment = globals
    private val locals: MutableMap<Expr, Int> = mutableMapOf()

    init {
        NativeFunctions.getAllNativeFunctions().forEach { (name, function) ->
            globals.define(name to function)
        }
    }

    fun interpret(statements: List<Stmt>, errorHandler: ErrorHandler) {
        try {
            statements.forEach(::execute)
        } catch (error: RuntimeError) {
            errorHandler.reportRuntimeError(error)
        }
    }

    override fun visit(expr: Assign): Any? {
        val value = evaluate(expr.value)
        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name to value)
        } else {
            globals.assign(expr.name to value)
        }
        return value
    }

    override fun visit(expr: Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            GREATER -> binaryOperation(expr.operator, left, right) { l, r -> l > r }
            GREATER_EQUAL -> binaryOperation(expr.operator, left, right) { l, r -> l >= r }
            LESS -> binaryOperation(expr.operator, left, right) { l, r -> l < r }
            LESS_EQUAL -> binaryOperation(expr.operator, left, right) { l, r -> l <= r }
            BANG_EQUAL -> !isEqual(left, right)
            EQUAL_EQUAL -> isEqual(left, right)
            MINUS -> binaryOperation(expr.operator, left, right, Double::minus)
            SLASH ->
                binaryOperation(expr.operator, left, right) { l, r ->
                    if (r == 0.0) throw RuntimeError(expr.operator, "Cannot divide by zero")
                    l / r
                }

            STAR -> binaryOperation(expr.operator, left, right, Double::times)
            PLUS ->
                if (left is Double && right is Double) left + right
                else if (left is String || right is String) left.toString() + right.toString()
                else
                    throw RuntimeError(
                        expr.operator,
                        "Operands must be both be numbers or strings"
                    )

            else -> null
        }
    }

    override fun visit(expr: Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments?.map(::evaluate)

        if (callee !is LoxCallable)
            throw RuntimeError(expr.paren, "Can only call functions and classes.")

        val function = callee
        if (arguments?.size != function.arity())
            throw RuntimeError(expr.paren, "Expect ${function.arity()} arguments but got ${arguments?.size}.")

        return function.call(this, arguments)
    }

    override fun visit(expr: Get): Any? {
        val obj = evaluate(expr.obj)
        return when (obj) {
            is LoxInstance -> obj.get(expr.name)
            is LoxClass -> obj.get(expr.name)
            else -> throw RuntimeError(expr.name, "Only instances have properties")
        }
    }

    override fun visit(expr: AnonymousFunction): Any? =
        LoxFunction(expr, environment, false)

    override fun visit(expr: Grouping): Any? = evaluate(expr.expression)

    override fun visit(expr: Literal): Any? = expr.value

    override fun visit(expr: Logical): Any? {
        val left = evaluate(expr.left)

        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visit(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj !is LoxInstance)
            throw RuntimeError(expr.name, "Only instances have fields")

        val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }

    override fun visit(expr: Super): Any? {
        val distance = locals[expr]
        val superClass = environment.getAt(distance!!, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance
        val method = superClass.findMethod(expr.method.lexeme)
            ?: throw RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'")

        return method.bind(obj)
    }

    override fun visit(expr: This): Any? = lookUpVariable(expr.keyword, expr)

    override fun visit(stmt: Expression) {
        evaluate(stmt.expression)
    }

    override fun visit(stmt: If) {
        val (condition, thenBranch, elseBranch) = stmt
        if (isTruthy(evaluate(condition)))
            execute(thenBranch)
        else if (elseBranch != null)
            execute(elseBranch)
    }

    override fun visit(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme to function)
    }

    override fun visit(stmt: Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visit(stmt: Return) {
        val value = if (stmt.value != null) evaluate(stmt.value) else null
        throw RuntimeReturn(value)
    }

    override fun visit(stmt: Var) {
        val value = if (stmt.initializer != null) evaluate(stmt.initializer) else null
        environment.define(stmt.name.lexeme to value)
    }

    override fun visit(stmt: While) {
        val (condition, body) = stmt
        while (isTruthy(evaluate(condition)))
            execute(body)
    }

    override fun visit(stmt: Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visit(stmt: Class) {
        var superClass: Any? = null
        if (stmt.superClass != null) {
            superClass = evaluate(stmt.superClass)
            if (superClass !is LoxClass)
                throw RuntimeError(stmt.superClass.name, "Super class must be a class")
        }

        environment.define(stmt.name.lexeme to null)

        if (stmt.superClass != null) {
            environment = Environment(environment)
            environment.define("super" to superClass)
        }

        val methods: Map<String, LoxFunction> = stmt.methods.associate {
            val method = it.name.lexeme
            method to LoxFunction(it, environment, method == "init")
        }

        val staticMethods: Map<String, LoxFunction> = stmt.staticMethods.associate {
            val method = it.name.lexeme
            method to LoxFunction(it, environment, false)
        }
        val klass = LoxClass(stmt.name.lexeme, superClass as LoxClass?, methods, staticMethods)
        if (stmt.superClass != null) environment = environment.enclosing!!

        environment.assign(stmt.name to klass)
    }

    override fun visit(expr: Unary): Any? {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visit(expr: Variable): Any? = lookUpVariable(expr.name, expr)

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        return if (distance != null) {
            environment.getAt(distance, name.lexeme)
        } else {
            globals[name]
        }
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            this.environment = previous
        }
    }

    private fun evaluate(expr: Expr?): Any? = expr?.accept(this)

    private fun execute(stmt: Stmt): Any = stmt.accept(this)

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    /**
     * As mentioned in the book (crafting interpreters) here we followed the same rule as Ruby false
     * and nil -> are falsey everything else is truthy
     *
     * book mentions that this rule is variable between languages js, python, php
     */
    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            else -> true
        }
    }

    private fun isEqual(left: Any?, right: Any?): Boolean =
        when {
            left == null && right == null -> true
            left == null || right == null -> false
            else -> left == right
        }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun binaryOperation(
        operator: Token,
        left: Any?,
        right: Any?,
        operation: (Double, Double) -> Any
    ): Any {
        checkNumberOperands(operator, left, right)
        return operation(left as Double, right as Double)
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"
        if (value is Double) {
            var text = value.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }
        return value.toString()
    }
}

