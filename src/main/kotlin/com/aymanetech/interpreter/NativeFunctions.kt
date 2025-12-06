package com.aymanetech.interpreter

import com.aymanetech.runtime.LoxCallable
import java.lang.System.currentTimeMillis

object NativeFunctions {

    fun getAllNativeFunctions(): Map<String, LoxCallable> = mapOf(
        "clock" to ClockFunction,
        "println" to PrintlnFunction,
        "scan" to ScanFunction,
        "input" to InputFunction
    )

    private object ClockFunction : LoxCallable {
        override fun arity() = 0
        override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any = currentTimeMillis() / 1000.0
        override fun toString() = "<native fn>"
    }

    private object PrintlnFunction : LoxCallable {
        override fun arity() = 1
        override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any = println(arguments?.first())
        override fun toString() = "<native fn>"
    }

    private object ScanFunction : LoxCallable {
        override fun arity() = 0
        override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any = readln()
        override fun toString() = "<native fn>"
    }

    private object InputFunction : LoxCallable {
        override fun arity() = 1
        override fun call(interpreter: Interpreter, arguments: List<Any?>?): Any {
            print(arguments?.first())
            return readln()
        }

        override fun toString() = "<native fn>"
    }
}