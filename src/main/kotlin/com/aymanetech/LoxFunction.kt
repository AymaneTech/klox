package com.aymanetech

import com.aymanetech.Stmt.Function

class LoxFunction(
    private val name: String?,
    private val params: List<Token>,
    private val body: List<Stmt>,
    private val closure: Environment
) : LoxCallable {

    /**
     * this constructor used with named functions
     */
    constructor(declaration: Function, closure: Environment) : this(
        declaration.name.lexeme,
        declaration.params,
        declaration.body,
        closure
    )

    /**
     * this constructor used with anonymous functions
     */
    constructor(function: Expr.AnonymousFunction, closure: Environment) : this(
        null,
        function.params,
        function.body,
        closure
    )

    override fun arity(): Int = params.size

    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>?
    ): Any? {
        val environment = Environment(closure)
        params.forEachIndexed { index, token -> environment.define(token.lexeme to arguments?.get(index)) }
        return try {
            interpreter.executeBlock(body, environment)
        } catch (returnValue: RuntimeReturn) {
            returnValue.value
        }
    }

    override fun toString(): String = name?.let { "<fn ${it}>" } ?: "<fn>"
}