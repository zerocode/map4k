package com.github.zerocode.map4k.configuration

import com.github.zerocode.map4k.configuration.TypeConversions.Companion.noopConverter
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

data class PropertyMap(
    val targetProperty: KProperty1<*, *>,
    val targetParameter: KParameter,
    val sourceResolution: SourceResolution,
    val conversion: TypeConverter
) {
    val targetPropertyName = targetProperty.name
    val targetPropertyClass = targetProperty.returnTypeClass
    val targetPropertyType = targetProperty.returnType
}

interface SourceType {
    val sourceType: KType
}

sealed class SourceResolution {
    abstract fun resolveValue(source: Any): Any?
}

data class NamedSourceResolution(
    val sourceProperty: KProperty1<*, *>
) : SourceResolution(), SourceType {
    override val sourceType: KType = sourceProperty.returnType

    override fun resolveValue(source: Any): Any? =
        sourceProperty.getter.call(source)
}

data class ConvertedSourceResolution(
    val sourceProperty: KProperty1<*, *>
) : SourceResolution(), SourceType {
    override val sourceType: KType = sourceProperty.returnType

    override fun resolveValue(source: Any): Any? =
        sourceProperty.getter.call(source)
}

data class GeneratedSourceResolution(
    val generator: Function1<Any?, *>
) : SourceResolution() {
    override fun resolveValue(source: Any): Any? =
        generator(source)
}

inline fun <reified TSource : Any, reified TSourceReturn : Any, reified TTarget : Any, reified TTargetReturn> namedPropertyMap(
    sourceProperty: KProperty1<TSource, TSourceReturn>,
    targetProperty: KProperty1<TTarget, TTargetReturn>
): PropertyMap {
    return PropertyMap(
        targetProperty = targetProperty,
        targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
        sourceResolution = NamedSourceResolution(sourceProperty = sourceProperty),
        conversion = noopConverter(TSourceReturn::class, TTargetReturn::class)
    )
}

inline fun <reified TSource : Any, reified TSourceReturn : Any, reified TTarget : Any, reified TTargetReturn> convertedPropertyMap(
    sourceProperty: KProperty1<TSource, TSourceReturn>,
    targetProperty: KProperty1<TTarget, TTargetReturn>,
    noinline converter: Function1<TSourceReturn, TTargetReturn>
): PropertyMap {
    return PropertyMap(
        targetProperty = targetProperty,
        targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
        sourceResolution = ConvertedSourceResolution(
            sourceProperty = sourceProperty
        ),
        conversion = SimpleTypeConverter(TSourceReturn::class, TTargetReturn::class, converter as (Any) -> Any)

    )
}

inline fun <reified TSource : Any, reified TTarget : Any, reified TTargetReturn> generatedPropertyMap(
    targetProperty: KProperty1<TTarget, TTargetReturn>,
    noinline customValueResolver: Function1<TSource, TTargetReturn>
): PropertyMap {
    return PropertyMap(
        targetProperty = targetProperty,
        targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
        sourceResolution = GeneratedSourceResolution(generator = customValueResolver as Function1<Any?, *>),
        conversion = noopConverter(TSource::class, TTargetReturn::class)
    )
}