package com.github.zerocode.map4k.configuration

import kotlin.reflect.KClass

data class TypeConverter(
    val sourceClass: KClass<*>,
    val targetClass: KClass<*>,
    val converter: (Any) -> Any
)

class TypeConversions(
    val typeConverters: Map<Pair<KClass<*>, KClass<*>>, TypeConverter> = emptyMap()
) {

    fun getConverter(sourceClass: KClass<*>, targetClass: KClass<*>): ((Any) -> Any)? =
        typeConverters[Pair(sourceClass, targetClass)]?.converter

    @Suppress("UNCHECKED_CAST")
    inline fun <reified TSource : Any, reified TTarget : Any> convert(source: TSource): TTarget =
        typeConverters[Pair(TSource::class, TTarget::class)]?.converter!!.invoke(source) as TTarget

    companion object {
        val IDENTITY_CONVERSION: Function1<Any?, *> = { x -> x }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified TSource : Any, reified TTarget : Any> typeConverter(noinline converter: (TSource) -> TTarget): TypeConverter =
    TypeConverter(TSource::class, TTarget::class, converter as (Any) -> Any)

fun typeConverters(vararg typeConverters: TypeConverter): TypeConversions =
    TypeConversions(typeConverters.map { (Pair(it.sourceClass, it.targetClass) to it) }.toMap())
