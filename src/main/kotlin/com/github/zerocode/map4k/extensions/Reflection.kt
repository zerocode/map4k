package com.github.zerocode.map4k.extensions

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties

val <T : Any> KClass<T>.declaredMemberPropertyNames: Collection<String>
    get() = this.declaredMemberProperties.map { it.name }

@Suppress("UNCHECKED_CAST")
val <T, R> KProperty1<T, R>.returnTypeClass: KClass<*>
    get() = this.returnType.classifier as KClass<*>

val KType.kClass: KClass<*>
    get() = this.classifier as KClass<*>

val KType.firstTypeArgument: KType
    get() = this.arguments.first().type!!

val KType.secondTypeArgument: KType
    get() = this.arguments[1].type!!