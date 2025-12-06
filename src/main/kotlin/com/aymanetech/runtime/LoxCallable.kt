package com.aymanetech.runtime

import com.aymanetech.interpreter.Interpreter

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>?): Any?
}