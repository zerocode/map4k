package com.github.zerocode.map4k.configuration

import kotlin.reflect.KClass

interface ValueConverter<in TSource : Any, TTarget : Any> {
    fun canConvert(source: KClass<*>, target: KClass<*>): Boolean
    fun convert(source: TSource, target: KClass<*>): TTarget
}