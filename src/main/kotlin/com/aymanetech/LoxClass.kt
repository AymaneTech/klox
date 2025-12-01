package com.aymanetech

class LoxClass(private val name: String) : LoxCallable {

    override fun arity(): Int = 0

    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>?
    ): Any? {
        return LoxInstance(this)
    }

    override fun toString(): String = name
}