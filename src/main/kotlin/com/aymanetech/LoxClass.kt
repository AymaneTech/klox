package com.aymanetech

class LoxClass(
    private val name: String,
    private val methods: Map<String, LoxFunction>
) : LoxCallable {

    override fun arity(): Int = 0

    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>?
    ): Any? {
        return LoxInstance(this)
    }

    fun findMethod(name: String): LoxFunction? = methods[name]

    override fun toString(): String = name
}