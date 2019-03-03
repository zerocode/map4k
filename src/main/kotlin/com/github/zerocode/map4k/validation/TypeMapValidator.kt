package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.AbstractDescriptor
import com.github.zerocode.map4k.ArrayDescriptor
import com.github.zerocode.map4k.DataClassDescriptor
import com.github.zerocode.map4k.ListDescriptor
import com.github.zerocode.map4k.MapDescriptor
import com.github.zerocode.map4k.ObjectDescriptor
import com.github.zerocode.map4k.PrimitiveDescriptor
import com.github.zerocode.map4k.TypeDescriptor
import com.github.zerocode.map4k.configuration.InvalidConfigException
import com.github.zerocode.map4k.configuration.MappingConfig
import com.github.zerocode.map4k.configuration.PropertyMap
import com.github.zerocode.map4k.configuration.TypeMap
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSuperclassOf

class TypeMapValidator(private val typeMap: TypeMap, private val mappingConfig: MappingConfig = MappingConfig()) {

    fun validate() {
        typeMap.targetClass.declaredMemberProperties.forEach {
            assertHasValidSource(it)
            assertHasCompatibleTypes(it)
        }
    }

    /**
    target property has named source property mapping
    target property has custom source property mapping
    target property has custom value resolver (TODO)
    target parameter is optional
     **/
    private fun assertHasValidSource(targetProperty: KProperty1<out Any, Any?>) {
        if (!hasValidSource(targetProperty)) {
            throw InvalidConfigException(
                "$invalidConfigMessage. No valid source property found for target property '${targetProperty.name}'. " +
                "Target properties must have a source property resolvable by name, a custom source mapping, or a custom value resolver. " +
                "Where no source is provided, the target property must be optional."
            )
        }
    }

    private fun hasValidSource(targetProperty: KProperty1<out Any, Any?>): Boolean =
        hasSourceMap(targetProperty) ||
        targetParameterIsOptional(targetProperty) ||
        targetPropertyHasNoConstructorParameter(targetProperty)

    private fun assertHasValidTarget(typeMap: TypeMap, targetProperty: KProperty1<out Any, Any?>) {

        /*
        / target property has matching constructor parameter by name
         */
    }

    private fun assertHasCompatibleTypes(targetProperty: KProperty1<out Any, Any?>) {
        typeMap.propertyMaps.firstOrNull { it.targetPropertyName == targetProperty.name }?.let { propertyMap ->
            if (!hasCompatibleTypes(propertyMap)) {
                throw InvalidConfigException(
                    "$invalidConfigMessage. Source and target have incompatible types: Source: ${propertyMap.sourceResolution}, Target: ${propertyMap.targetPropertyClass}. "
                )
            }
        }
        /*
        / target property type is same as source type
        / target property type is superclass of source type
        / target property has custom type conversion (PropertyMap, TypeMap, Global)
        / target property has TypeMap
        / target property has SubClassTypeMap
         */
    }

    private fun hasCompatibleTypes(propertyMap: PropertyMap): Boolean =
        hasSameOrRelatedType(propertyMap) ||
        hasConversion(propertyMap)

    private fun hasConversion(propertyMap: PropertyMap): Boolean =
        hasSameOrRelatedType(propertyMap.conversion.targetTypeDescriptor, propertyMap.targetDescriptor) ||
        hasGlobalConversion(propertyMap.sourceResolution.sourceDescriptor, propertyMap.targetDescriptor)

    private fun hasSameOrRelatedType(propertyMap: PropertyMap): Boolean =
        hasSameOrRelatedType(propertyMap.sourceResolution.sourceDescriptor, propertyMap.targetDescriptor)

    private fun hasSameOrRelatedType(sourceDescriptor: TypeDescriptor, targetDescriptor: TypeDescriptor): Boolean {
        return when (targetDescriptor) {
            is DataClassDescriptor -> targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass)
            is ObjectDescriptor -> targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass)
            is PrimitiveDescriptor -> targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass)
            is AbstractDescriptor -> targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass)
            is ListDescriptor -> {
                sourceDescriptor is ListDescriptor &&
                targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass) &&
                hasSameOrRelatedType(sourceDescriptor.typeParameter, targetDescriptor.typeParameter)
            }
            is MapDescriptor ->
                sourceDescriptor is MapDescriptor &&
                targetDescriptor.kClass.isSuperclassOf(sourceDescriptor.kClass) &&
                hasSameOrRelatedType(sourceDescriptor.typeParameter, targetDescriptor.typeParameter)
            is ArrayDescriptor -> TODO()
        }
    }

    private fun hasGlobalConversion(sourceDescriptor: TypeDescriptor, targetDescriptor: TypeDescriptor): Boolean {
        return when (targetDescriptor) {
            is DataClassDescriptor -> mappingConfig.typeMapFor(sourceDescriptor.kClass, targetDescriptor.kClass) != null
            is ObjectDescriptor -> mappingConfig.typeMapFor(sourceDescriptor.kClass, targetDescriptor.kClass) != null
            is PrimitiveDescriptor -> mappingConfig.userDefinedTypeConversions.getConverter(sourceDescriptor.kClass, targetDescriptor.kClass) != null
            is AbstractDescriptor -> mappingConfig.typeMapFor(sourceDescriptor.kClass, targetDescriptor.kClass) != null
            is ListDescriptor -> {
                sourceDescriptor is ListDescriptor &&
                hasGlobalConversion(sourceDescriptor.typeParameter, targetDescriptor.typeParameter)
            }
            is MapDescriptor -> {
                sourceDescriptor is MapDescriptor &&
                hasGlobalConversion(sourceDescriptor.typeParameter, targetDescriptor.typeParameter)
            }
            is ArrayDescriptor -> TODO()
        }
    }

    private fun hasSourceMap(targetProperty: KProperty1<out Any, Any?>): Boolean =
        typeMap.propertyMaps.any { it.targetPropertyName == targetProperty.name }

    private fun targetParameterIsOptional(targetProperty: KProperty1<out Any, Any?>): Boolean =
        typeMap.targetParameters.firstOrNull { it.name == targetProperty.name }?.isOptional ?: false

    private fun targetPropertyHasNoConstructorParameter(targetProperty: KProperty1<out Any, Any?>): Boolean =
        typeMap.targetParameters.none { it.name == targetProperty.name }

    private val invalidConfigMessage =
        "${typeMap.sourceClass.simpleName} cannot be mapped to ${typeMap.targetClass.simpleName}"
}