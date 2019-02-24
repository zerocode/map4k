package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.configuration.InvalidConfigException
import com.github.zerocode.map4k.configuration.TypeMap
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

class TypeMapValidator(private val typeMap: TypeMap) {

    fun validate() {
        typeMap.targetClass.declaredMemberProperties.forEach {
            assertHasValidSource(it)
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
        targetParameterIsOptional(targetProperty)

    private fun assertHasValidTarget(typeMap: TypeMap, targetProperty: KProperty1<out Any, Any?>) {

        /*
        / target property has matching constructor parameter by name
         */
    }

    private fun assertHasValidType(typeMap: TypeMap, targetProperty: KProperty1<out Any, Any?>) {

        /*
        / target property type is same as source type
        / target property type is superclass of source type
        / target property has custom type conversion (PropertyMap, TypeMap, Global)
        / target property has TypeMap
        / target property has SubClassTypeMap
         */
    }

    private fun hasSourceMap(targetProperty: KProperty1<out Any, Any?>): Boolean =
        typeMap.propertyMaps.any { it.targetPropertyName == targetProperty.name }

    private fun targetParameterIsOptional(targetProperty: KProperty1<out Any, Any?>): Boolean =
        typeMap.targetParameters.firstOrNull { it.name == targetProperty.name }?.isOptional
        ?: throw InvalidConfigException("$invalidConfigMessage. Constructor parameter for target property '${targetProperty.name}' is not found.")

    private val invalidConfigMessage =
        "${typeMap.sourceClass.simpleName} cannot be mapped to ${typeMap.targetClass.simpleName}"
}