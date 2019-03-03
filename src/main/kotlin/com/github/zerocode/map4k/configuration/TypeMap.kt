package com.github.zerocode.map4k.configuration

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

data class TypeMap(
    val sourceClass: KClass<*>,
    val targetClass: KClass<*>,
    val propertyMaps: Collection<PropertyMap> = emptyList()
) {
    val targetParameters: Collection<KParameter> =
        targetClass.primaryConstructor?.parameters ?: emptyList()

    fun isMapFor(sourceClassToFind: KClass<*>, targetClassToFind: KClass<*>): Boolean =
        this.sourceClass.isSubclassOf(sourceClassToFind) && this.targetClass.isSubclassOf(targetClassToFind)

    fun createTargetWith(parameterValues: Map<KParameter, Any?>): Any {
        return targetClass.objectInstance ?: targetClass.primaryConstructor!!.callBy(parameterValues)
    }
}