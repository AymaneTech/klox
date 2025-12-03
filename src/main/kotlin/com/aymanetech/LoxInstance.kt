package com.aymanetech

class LoxInstance(private val klass: LoxClass) {
    private val fields: MutableMap<String, Any?> = mutableMapOf()

    override fun toString(): String = "${klass.toString()} instance"

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme))
            return fields[name.lexeme]

        val method = klass.findMethod(name.lexeme)
        if (method != null) return method

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")

    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    private fun validateFieldExistence(name: Token) {
    }
}