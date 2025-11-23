package com.aymanetech

import com.aymanetech.Expr.*
import com.aymanetech.Lexer.runtimeError
import com.aymanetech.Stmt.*
import com.aymanetech.TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private var environment: Environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach(::execute)
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    override fun visit(expr: Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name to value)
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

    override fun visit(stmt: Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
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

    override fun visit(expr: Variable): Any? = environment[expr.name]

    private fun evaluate(expr: Expr?): Any? = expr?.accept(this)

    private fun execute(stmt: Stmt): Any = stmt.accept(this)

    private fun executeBlock(statements: List<Stmt>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach(::execute)
        } finally {
            this.environment = previous
        }
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

// TODO: I can improve this using kotlin algebratic types feature
class RuntimeError(val token: Token, message: String) : RuntimeException(message)
