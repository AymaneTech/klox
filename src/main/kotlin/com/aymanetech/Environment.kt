package com.aymanetech

data class Environment(val values: MutableMap<String, Any?> = HashMap()) {

    fun define(definition: Pair<String, Any?>) {
        val (name, value) = definition
        values[name] = value
    }

    operator fun get(name: Token): Any? {
        if (!values.containsKey(name.lexeme))
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")

        return values[name.lexeme]
    }

    fun assign(assignment: Pair<Token, Any?>) {
        val (name, value) = assignment
        if (!values.containsKey(name.lexeme))
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")

        values[name.lexeme] = value
    }
}
