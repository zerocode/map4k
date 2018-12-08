package com.github.pseudometa.map4k.conversions

import kotlin.reflect.KClass

class ValueConverterRegistry(private vararg val valueConverters: ValueConverter<*, *>) {
    fun containsConverter(source: KClass<*>, target: KClass<*>): Boolean = valueConverters.any { it.canConvert(source, target) }

    @Suppress("UNCHECKED_CAST")
    fun getConverter(source: KClass<*>, target: KClass<*>): ValueConverter<Any, Any> =
        valueConverters.first { it.canConvert(source, target) } as ValueConverter<Any, Any>
}