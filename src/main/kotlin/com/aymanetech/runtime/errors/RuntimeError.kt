package com.aymanetech.runtime.errors

import com.aymanetech.lexer.Token

class RuntimeError(val token: Token, message: String) : RuntimeException(message)