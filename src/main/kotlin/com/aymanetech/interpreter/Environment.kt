package com.aymanetech.interpreter

import com.aymanetech.lexer.Token
import com.aymanetech.runtime.errors.RuntimeError

data class Environment(
    val enclosing: Environment? = null,
    val values: MutableMap<String, Any?> = HashMap()
) {

    fun define(definition: Pair<String, Any?>) {
        val (name, value) = definition
        values[name] = value
    }

    operator fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme))
            return values[name.lexeme]

        if (enclosing != null)
            return enclosing[name]

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")

    }

    fun getAt(distance: Int, name: String) = ancestor(distance).values[name]

    fun assign(assignment: Pair<Token, Any?>) {
        val (name, value) = assignment
        if (values.containsKey(name.lexeme)){
            values[name.lexeme] = value
            return
        }

        if (enclosing != null){
            enclosing.assign(assignment)
            return
        }

        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assignAt(distance: Int, assignment: Pair<Token, Any?>) {
        val (name, value) = assignment
        ancestor(distance).values[name.lexeme] = value
    }

    private fun ancestor(distance: Int): Environment {
        var env = this
        for (i in 0 until distance) {
            env = env.enclosing!!
        }
        return env
    }
}
