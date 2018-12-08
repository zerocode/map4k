package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.AbstractDescriptor
import com.github.zerocode.map4k.configuration.DataClassDescriptor
import com.github.zerocode.map4k.configuration.ListDescriptor
import com.github.zerocode.map4k.configuration.MapConfig
import com.github.zerocode.map4k.configuration.PrimitiveDescriptor
import com.github.zerocode.map4k.configuration.PropertyMap
import com.github.zerocode.map4k.configuration.TypeDescriptor
import com.github.zerocode.map4k.validation.MapConfigValidator
import kotlin.reflect.KClass

class MapperV3(private val config: MapConfig) {

    init {
        MapConfigValidator(config).validate()
    }

    inline fun <reified TTarget : Any> map(source: Any): TTarget =
        map(source, TTarget::class)

    fun <TTarget : Any> map(source: Any, targetClass: KClass<TTarget>): TTarget {
        @Suppress("UNCHECKED_CAST")
        return mapInternal(source, targetClass) as TTarget
    }

    private fun mapInternal(source: Any, targetClass: KClass<*>): Any {
        val sourceClass = source::class
        val typeMap = config.typeMapFor(sourceClass, targetClass)
                      ?: throw MappingException("No TypeMap found for ${sourceClass.simpleName} and ${targetClass.simpleName}.")

        val parameterValues = typeMap.propertyMaps.map { it.targetParameter to resolvePropertyValue(it.sourceResolution.resolveValue(source), it) }.toMap()
        return typeMap.createTargetWith(parameterValues)
    }

    private fun resolvePropertyValue(source: Any?, propertyMap: PropertyMap): Any? {
        val targetDescriptor = TypeDescriptor.resolve(propertyMap.targetPropertyClass, propertyMap.targetPropertyType)
        val resolvedValue = when (targetDescriptor) {
            is PrimitiveDescriptor -> source
            is DataClassDescriptor, is AbstractDescriptor -> source?.let { mapInternal(it, targetDescriptor.kClass) }
            is ListDescriptor -> source?.let { resolveValue(it, targetDescriptor) }
        }
        return propertyMap.conversion(resolvedValue)
    }

    private fun resolveValue(source: Any, targetDescriptor: TypeDescriptor): Any {
        val conversion = config.typeConversions.getConverter(source::class, targetDescriptor.kClass)
        val resolvedValue = when (targetDescriptor) {
            is PrimitiveDescriptor -> source
            is DataClassDescriptor, is AbstractDescriptor -> mapInternal(source, targetDescriptor.kClass)
            is ListDescriptor -> (source as List<*>).map { resolveValue(it!!, targetDescriptor.typeParameter) }
        }
        return conversion?.invoke(resolvedValue) ?: resolvedValue
    }
}

class MappingException(message: String) : Exception(message)