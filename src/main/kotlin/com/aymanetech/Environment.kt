package com.aymanetech

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
}
