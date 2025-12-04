package com.aymanetech

class LoxFunction(
    private val declaration: LoxCallableDeclaration,
    private val closure: Environment
) : LoxCallable {

    constructor(declaration: Stmt.Function, closure: Environment)
            : this(FunctionDecl(declaration), closure)

    constructor(declaration: Expr.AnonymousFunction, closure: Environment)
            : this(AnonymousDecl(declaration), closure)


    fun bind(instance: LoxInstance): LoxFunction {
        val env = Environment(closure)
        env.define("this" to instance)
        return LoxFunction(declaration, env)
    }

    override fun arity(): Int = declaration.params.size

    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>?
    ): Any? {
        val environment = Environment(closure)
        declaration.params.forEachIndexed { index, token -> environment.define(token.lexeme to arguments?.get(index)) }
        return try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: RuntimeReturn) {
            returnValue.value
        }
    }

    override fun toString(): String = declaration.name?.let { "<fn ${it}>" } ?: "<fn>"
}

sealed interface LoxCallableDeclaration {
    val params: List<Token>
    val body: List<Stmt>
    val name: String?
}

data class FunctionDecl(
    val stmt: Stmt.Function
) : LoxCallableDeclaration {
    override val params = stmt.params
    override val body = stmt.body
    override val name = stmt.name.lexeme
}

data class AnonymousDecl(
    val expr: Expr.AnonymousFunction
) : LoxCallableDeclaration {
    override val params = expr.params
    override val body = expr.body
    override val name: String? = null
}
