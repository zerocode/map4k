package com.github.zerocode.map4k.configuration

import com.github.zerocode.map4k.TypeDescriptor
import com.github.zerocode.map4k.extensions.returnTypeClass
import com.github.zerocode.map4k.typeDescriptor
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.reflect

data class PropertyMap(
    val targetProperty: KProperty1<*, *>,
    val targetParameter: KParameter,
    val sourceResolution: SourceResolution,
    val conversion: TypeConverter
) {
    val targetPropertyName = targetProperty.name
    val targetPropertyClass = targetProperty.returnTypeClass
    val targetDescriptor = typeDescriptor(targetPropertyClass, targetProperty.returnType)
}

interface SourceType {
    val sourceType: KType
}

sealed class SourceResolution {
    abstract val sourceDescriptor: TypeDescriptor
    abstract fun resolveValue(source: Any): Any?
}

data class NamedSourceResolution(val sourceProperty: KProperty1<*, *>) : SourceResolution(), SourceType {

    override val sourceDescriptor: TypeDescriptor =
        typeDescriptor(sourceProperty.returnTypeClass, sourceProperty.returnType)

    override val sourceType: KType =
        sourceProperty.returnType

    override fun resolveValue(source: Any): Any? =
        sourceProperty.getter.call(source)
}

data class ConvertedSourceResolution(val sourceProperty: KProperty1<*, *>) : SourceResolution(), SourceType {

    override val sourceDescriptor: TypeDescriptor =
        typeDescriptor(sourceProperty.returnTypeClass, sourceProperty.returnType)

    override val sourceType: KType =
        sourceProperty.returnType

    override fun resolveValue(source: Any): Any? =
        sourceProperty.getter.call(source)
}

data class GeneratedSourceResolution(val generator: Function1<Any?, *>) : SourceResolution() {

    override val sourceDescriptor: TypeDescriptor =
        typeDescriptor(generator.reflect()!!.returnType.classifier as KClass<*>, generator.reflect()!!.returnType)

    override fun resolveValue(source: Any): Any? =
        generator(source)
}