package com.github.zerocode.map4k.configuration

import com.github.zerocode.map4k.validation.TypeMapValidator
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

class MappingConfig(
    private val userDefinedTypeMaps: Collection<TypeMap> = emptyList(),
    val userDefinedTypeConversions: TypeConversions = TypeConversions(),
    private val mappingOptions: MappingOptions = MappingOptions()
) {
    val typeMaps: Collection<TypeMap>
        get() = userDefinedTypeMaps + dynamicTypeMaps + identityTypeMaps

    private val dynamicTypeMaps = mutableListOf<TypeMap>()
    private val identityTypeMaps = mutableListOf<TypeMap>()

    fun converterFor(sourceClass: KClass<*>, targetClass: KClass<*>): TypeConverter? =
        userDefinedTypeConversions.getConverter(sourceClass, targetClass)

    fun typeMapFor(sourceClass: KClass<*>, targetClass: KClass<*>): TypeMap? {
        val sourceTarget = SourceTarget(sourceClass, targetClass)
        return getTypeMap(sourceTarget)
               ?: getIdentityTypeMap(sourceTarget)
               ?: getDynamicTypeMap(sourceTarget)
    }

    private fun getTypeMap(sourceTarget: SourceTarget) =
        typeMaps.firstOrNull { it.isMapFor(sourceTarget.sourceClass, sourceTarget.targetClass) }

    private fun getDynamicTypeMap(sourceTarget: SourceTarget): TypeMap? =
        when (mappingOptions.dynamicTypeMapping) {
            is Enabled -> if (!sourceTarget.areSameOrRelatedTypes && !sourceTarget.targetClass.isAbstract) {
                TypeMapBuilder(sourceTarget.sourceClass, sourceTarget.targetClass).build(userDefinedTypeConversions, mappingOptions).also {
                    TypeMapValidator(it, this).validate()
                    dynamicTypeMaps.add(it)
                }
            } else null
            is Disabled -> null
        }

    private fun getIdentityTypeMap(sourceTarget: SourceTarget): TypeMap? =
        when (mappingOptions.identityTypeMapping) {
            is Enabled -> if (sourceTarget.areSameOrRelatedTypes) {
                TypeMapBuilder(sourceTarget.sourceClass, sourceTarget.sourceClass).build(userDefinedTypeConversions, mappingOptions).also {
                    TypeMapValidator(it, this).validate()
                    identityTypeMaps.add(it)
                }
            } else null
            is Disabled -> null
        }
}

class InvalidConfigException(message: String) : Exception(message)

data class SourceTarget(val sourceClass: KClass<*>, val targetClass: KClass<*>) {
    val areSameOrRelatedTypes: Boolean =
        !sourceClass.isSubclassOf(Collection::class) &&
        !targetClass.isSubclassOf(Collection::class) &&
        targetClass.isSuperclassOf(sourceClass)

    companion object {
        inline fun <reified TSource : Any, reified TTarget : Any> create() =
            SourceTarget(TSource::class, TTarget::class)
    }
}
