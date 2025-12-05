package com.aymanetech

class LoxClass(
    private val name: String,
    private val methods: Map<String, LoxFunction>,
    private val staticMethods: Map<String, LoxFunction>
) : LoxCallable{

    val metaInstance = LoxInstance(this)

    override fun arity(): Int {
        val initializer = findMethod("init")
        return initializer?.arity() ?: 0
    }

    override fun call(
        interpreter: Interpreter,
        arguments: List<Any?>?
    ): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    fun findMethod(name: String): LoxFunction? = methods[name]

    fun get(token: Token): Any? {
        val method = findStaticMethod(token.lexeme)
        if (method != null) return method.bind(metaInstance)

        throw RuntimeError(token, "Undefined property '${token.lexeme}' on class '$name'.")
    }

    fun findStaticMethod(name: String): LoxFunction? = staticMethods[name]

    override fun toString(): String = name
}