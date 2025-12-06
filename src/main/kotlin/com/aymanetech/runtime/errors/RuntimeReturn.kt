package com.aymanetech.runtime.errors

class RuntimeReturn(val value: Any?) : RuntimeException(
    null,
    null,
    false,
    false
)