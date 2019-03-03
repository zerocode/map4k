package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.MappingConfig
import com.github.zerocode.map4k.configuration.PropertyMap
import com.github.zerocode.map4k.configuration.TypeMap
import com.github.zerocode.map4k.validation.MapConfigValidator
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

class Mapper(private val mappingConfig: MappingConfig) {

    init {
        MapConfigValidator(mappingConfig).validate()
    }

    inline fun <reified TTarget : Any> map(source: Any): TTarget =
        map(source, TTarget::class)

    fun <TTarget : Any> map(source: Any, targetClass: KClass<TTarget>): TTarget {
        @Suppress("UNCHECKED_CAST")
        return mapInternal(source, targetClass) as TTarget
    }

    private fun mapInternal(source: Any, targetClass: KClass<*>): Any {
        val sourceClass = source::class
        val typeMap = typeMapFor(sourceClass, targetClass)
        val parameterValues = typeMap.propertyMaps.map { it.targetParameter to resolvePropertyValue(it.sourceResolution.resolveValue(source), it) }.toMap()
        return createTarget(typeMap, parameterValues, source, targetClass)
    }

    private fun createTarget(typeMap: TypeMap, parameterValues: Map<KParameter, Any?>, source: Any, targetClass: KClass<*>): Any {
        try {
            return typeMap.createTargetWith(parameterValues)
        } catch (ex: Exception) {
            throw MappingException("Source: $source. " + System.lineSeparator() +
                                   "Target: $targetClass. " + System.lineSeparator() +
                                   "ParameterValues: $parameterValues." + System.lineSeparator() +
                                   "Exception: $ex."
            )
        }
    }

    private fun typeMapFor(sourceClass: KClass<*>, targetClass: KClass<*>): TypeMap {
        return mappingConfig.typeMapFor(sourceClass, targetClass)
               ?: throw MappingException("No TypeMap found for ${sourceClass.simpleName} and ${targetClass.simpleName}.")
    }

    private fun resolvePropertyValue(source: Any?, propertyMap: PropertyMap): Any? {
        if (source == null) {
            return null
        }
        val resolvedValue = when (propertyMap.targetDescriptor) {
                                is PrimitiveDescriptor -> source
                                is ObjectDescriptor -> propertyMap.targetDescriptor.instance
                                is DataClassDescriptor, is AbstractDescriptor -> mapInternal(source, propertyMap.targetDescriptor.kClass)
                                is ListDescriptor, is MapDescriptor, is ArrayDescriptor -> resolveValue(source, propertyMap.targetDescriptor)
                            } ?: return null
        return propertyMap.conversion.convert(resolvedValue, propertyMap.targetDescriptor.kClass)
    }

    private fun resolveValue(source: Any?, targetDescriptor: TypeDescriptor): Any? {
        if (source == null) {
            return null
        }
        val conversion = mappingConfig.converterFor(source::class, targetDescriptor.kClass)
        val resolvedValue = when (targetDescriptor) {
            is PrimitiveDescriptor -> source
            is ObjectDescriptor -> targetDescriptor.instance
            is DataClassDescriptor -> mapInternal(source, targetDescriptor.kClass)
            is AbstractDescriptor -> mapInternal(source, resolveConcreteType(source::class, targetDescriptor.kClass))
            is ListDescriptor -> (source as List<*>).map { resolveValue(it, targetDescriptor.typeParameter) }
            is MapDescriptor -> (source as Map<*, *>).map { it.key to resolveValue(it.value, targetDescriptor.typeParameter) }.toMap()
            is ArrayDescriptor -> TODO()
        } ?: return null
        return conversion?.convert(resolvedValue, targetDescriptor.kClass) ?: resolvedValue
    }

    private fun resolveConcreteType(sourceClass: KClass<*>, targetClass: KClass<*>): KClass<*> {
        return targetClass
    }
}

class MappingException(message: String) : Exception(message)