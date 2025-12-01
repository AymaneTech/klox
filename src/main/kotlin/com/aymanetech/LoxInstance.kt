package com.aymanetech

class LoxInstance(private val klass: LoxClass) {
    private val fields: MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String = "${klass.toString()} instance"

    fun get(name: Token): Any? {
        validateFieldExistence(name)
        return fields[name.lexeme]
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    private fun validateFieldExistence(name: Token) {
        if (!fields.containsKey(name.lexeme))
            throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }
}