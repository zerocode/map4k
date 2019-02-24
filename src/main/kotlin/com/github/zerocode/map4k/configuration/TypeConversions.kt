package com.github.zerocode.map4k.configuration

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface TypeConverter {
    val sourceBaseClass: KClass<*>
    val targetBaseClass: KClass<*>

    fun canConvert(sourceClass: KClass<*>, targetClass: KClass<*>): Boolean =
        sourceClass.isSubclassOf(sourceBaseClass) && targetClass.isSubclassOf(targetBaseClass)

    fun convert(source: Any, targetClass: KClass<*>): Any
}

data class NoopTypeConverter(
    override val sourceBaseClass: KClass<*>,
    override val targetBaseClass: KClass<*>
) : TypeConverter {
    override fun convert(source: Any, targetClass: KClass<*>): Any = source
}

data class SimpleTypeConverter(
    override val sourceBaseClass: KClass<*>,
    override val targetBaseClass: KClass<*>,
    val converter: (Any) -> Any
) : TypeConverter {
    override fun convert(source: Any, targetClass: KClass<*>): Any =
        converter(source)
}

data class DerivedTargetTypeConverter(
    override val sourceBaseClass: KClass<*>,
    override val targetBaseClass: KClass<*>,
    val converter: (Any, KClass<*>) -> Any
) : TypeConverter {
    override fun convert(source: Any, targetClass: KClass<*>): Any =
        converter(source, targetClass)
}

data class TypeConversions(val typeConverters: List<TypeConverter> = emptyList()) {

    fun getConverter(sourceClass: KClass<*>, targetClass: KClass<*>): TypeConverter? =
        typeConverters.firstOrNull { it.canConvert(sourceClass, targetClass) }

    companion object {
        fun noopConverter(sourceClass: KClass<*>, targetClass: KClass<*>): TypeConverter =
            NoopTypeConverter(sourceClass, targetClass)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified TSource : Any, reified TTarget : Any> typeConverter(noinline converter: (TSource) -> TTarget): TypeConverter =
    SimpleTypeConverter(TSource::class, TTarget::class, converter as (Any) -> Any)

@Suppress("UNCHECKED_CAST")
inline fun <reified TSource : Any, reified TTarget : Any> typeConverter(noinline converter: (TSource, KClass<TTarget>) -> TTarget): TypeConverter =
    DerivedTargetTypeConverter(TSource::class, TTarget::class, converter as (Any, KClass<*>) -> Any)

fun typeConverters(vararg typeConverters: TypeConverter): TypeConversions =
    TypeConversions(typeConverters.toList())
