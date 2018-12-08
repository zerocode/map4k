package com.github.pseudometa.map4k.conversions

data class TypeMappingException(override val message: String, override val cause: Throwable? = null) : RuntimeException(message, cause)