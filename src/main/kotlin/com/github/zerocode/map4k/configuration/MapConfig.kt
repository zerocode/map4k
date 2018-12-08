package com.github.zerocode.map4k.configuration

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

data class TypeMap(
    val sourceClass: KClass<*>,
    val targetClass: KClass<*>,
    val customPropertyMaps: Collection<PropertyMap> = emptyList()
) {

    val propertyMaps: Collection<PropertyMap> by lazy {
        targetClass.declaredMemberProperties
            .filterNot { customPropertyMaps.map { it.targetPropertyName }.contains(it.name) }
            .mapNotNull { targetProperty ->
                sourceClass.declaredMemberProperties.firstOrNull { it.name == targetProperty.name }?.let { sourceProperty ->
                    PropertyMap(
                        targetProperty = targetProperty,
                        targetParameter = targetClass.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
                        sourceResolution = NamedSourceResolution(sourceProperty),
                        conversion = TypeConversions.IDENTITY_CONVERSION // TODO - should look up global conversions
                    )
                }
            } + customPropertyMaps
    }

    val targetParameters: Collection<KParameter> =
        targetClass.primaryConstructor?.parameters ?: emptyList()

    fun isMapFor(sourceClassToFind: KClass<*>, targetClassToFind: KClass<*>): Boolean =
        this.sourceClass.isSubclassOf(sourceClassToFind) && this.targetClass.isSubclassOf(targetClassToFind)

    fun createTargetWith(parameterValues: Map<KParameter, Any?>): Any {
        return targetClass.primaryConstructor!!.callBy(parameterValues)
    }
}

class MapConfig(
    val typeMaps: Collection<TypeMap>,
    val typeConversions: TypeConversions = TypeConversions(),
    val options: MapConfigOptions = MapConfigOptions()
) {

    fun typeMapFor(sourceClass: KClass<*>, targetClass: KClass<*>): TypeMap? =
        configuredTypeMap(sourceClass, targetClass)
        ?: dynamicTypeMap(sourceClass, targetClass)
        ?: identityTypeMap(sourceClass, targetClass)

    private fun configuredTypeMap(sourceClass: KClass<*>, targetClass: KClass<*>) =
        typeMaps.firstOrNull { it.isMapFor(sourceClass, targetClass) }

    private fun dynamicTypeMap(sourceClass: KClass<*>, targetClass: KClass<*>): TypeMap? =
        when (options.dynamicTypeMapping) {
            is Enabled -> TODO()
            is Disabled -> null
        }

    private fun identityTypeMap(sourceClass: KClass<*>, targetClass: KClass<*>): TypeMap? =
        when (options.identityTypeMapping) {
            is Enabled -> if (areSameOrRelatedTypes(sourceClass, targetClass)) TypeMap(sourceClass, sourceClass) else null
            is Disabled -> null
        }

    private fun areSameOrRelatedTypes(sourceClass: KClass<*>, targetClass: KClass<*>): Boolean {
        return !sourceClass.isSubclassOf(Collection::class) &&
               !targetClass.isSubclassOf(Collection::class) &&
               targetClass.isSuperclassOf(sourceClass)
    }
}

sealed class OptionSetting
object Enabled : OptionSetting()
object Disabled : OptionSetting()

data class MapConfigOptions(
    val dynamicTypeMapping: OptionSetting = Disabled,
    val identityTypeMapping: OptionSetting = Disabled
)

class InvalidConfigException(message: String) : Exception(message)

val <T : Any> KClass<T>.declaredMemberPropertyNames: Collection<String>
    get() = this.declaredMemberProperties.map { it.name }

@Suppress("UNCHECKED_CAST")
val <T, R> KProperty1<T, R>.returnTypeClass: KClass<*>
    get() = this.returnType.classifier as KClass<*>

val KType.kClass: KClass<*>
    get() = this.classifier as KClass<*>

val KType.firstTypeArgument: KType
    get() = this.arguments.first().type!!

fun unwrapTypeParameters(typeParameter: KType): TypeDescriptor = when {
    typeParameter.kClass.isSubclassOf(List::class) -> ListDescriptor(typeParameter.kClass, unwrapTypeParameters(typeParameter.firstTypeArgument))
    else -> TypeDescriptor.resolve(typeParameter.kClass)
}

sealed class TypeDescriptor(open val kClass: KClass<out Any>) {
    companion object {
        fun resolve(targetClass: KClass<*>, targetParameterType: KType? = null): TypeDescriptor = when {
            targetClass.isData -> DataClassDescriptor(targetClass)
            targetClass.isSubclassOf(List::class) -> ListDescriptor(targetClass, unwrapTypeParameters(targetParameterType!!.firstTypeArgument))
            targetClass.isAbstract -> AbstractDescriptor(targetClass)
            else -> PrimitiveDescriptor(targetClass)
        }
    }
}

data class DataClassDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class PrimitiveDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class AbstractDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class ListDescriptor(override val kClass: KClass<out Any>, val typeParameter: TypeDescriptor) : TypeDescriptor(kClass)

data class TypeMapBuilder(
    val sourceClass: KClass<*>,
    val targetClass: KClass<*>,
    val subTypeMapBuilders: List<TypeMapBuilder> = emptyList(),
    val propertyMaps: List<PropertyMap> = emptyList()
) {
    inline fun <reified TSource : Any, reified TSourceReturn : Any, reified TTarget : Any, reified TTargetReturn> propertyMap(
        sourceProperty: KProperty1<TSource, TSourceReturn>,
        targetProperty: KProperty1<TTarget, TTargetReturn>
    ): TypeMapBuilder {
        return this.copy(
            propertyMaps = this.propertyMaps + PropertyMap(
                targetProperty = targetProperty,
                targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
                sourceResolution = NamedSourceResolution(sourceProperty),
                conversion = TypeConversions.IDENTITY_CONVERSION
            ))
    }

    inline fun <reified TSource : Any, reified TSourceReturn : Any, reified TTarget : Any, reified TTargetReturn> propertyMap(
        sourceProperty: KProperty1<TSource, TSourceReturn>,
        targetProperty: KProperty1<TTarget, TTargetReturn>,
        noinline converter: Function1<TSourceReturn, TTargetReturn>
    ): TypeMapBuilder {
        return this.copy(
            propertyMaps = this.propertyMaps + PropertyMap(
                targetProperty = targetProperty,
                targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
                sourceResolution = ConvertedSourceResolution(
                    sourceProperty = sourceProperty
                ),
                conversion = converter as Function1<Any?, *>
            ))
    }

    inline fun <reified TSource : Any, reified TTarget : Any, reified TTargetReturn> propertyMap(
        targetProperty: KProperty1<TTarget, TTargetReturn>,
        noinline generator: Function1<TSource, TTargetReturn>
    ): TypeMapBuilder {
        return this.copy(
            propertyMaps = this.propertyMaps + PropertyMap(
                targetProperty = targetProperty,
                targetParameter = TTarget::class.primaryConstructor?.parameters?.first { it.name == targetProperty.name }!!,
                sourceResolution = GeneratedSourceResolution(generator = generator as Function1<Any?, *>),
                conversion = TypeConversions.IDENTITY_CONVERSION
            ))
    }

    // TODO test & refactor
    fun build(
        typeConversions: TypeConversions = TypeConversions(),
        options: MapConfigOptions = MapConfigOptions()
    ): TypeMap {
        val propertyMapsWithConversions = propertyMaps.map {
            when (it.sourceResolution) {
                is NamedSourceResolution -> {
                    val converter = typeConversions.getConverter(it.sourceResolution.sourceProperty.returnTypeClass, it.targetPropertyClass)
                    if (converter != null) {
                        it.copy(conversion = converter as Function1<Any?, *>)
                    } else {
                        it
                    }
                }
                else -> it
            }
        }
        return TypeMap(sourceClass, targetClass, propertyMapsWithConversions)
    }
}

inline fun <reified TSource : Any, reified TTarget : Any> typeMap(): TypeMapBuilder {
    return TypeMapBuilder(TSource::class, TTarget::class)
}

fun config(
    vararg typeMaps: TypeMapBuilder,
    typeConversions: TypeConversions = TypeConversions(),
    options: MapConfigOptions = MapConfigOptions()
): MapConfig =
    MapConfig(typeMaps.map { it.build(typeConversions, options) }, typeConversions, options)

fun options(dynamicTypeMapping: OptionSetting = Disabled, identityTypeMapping: OptionSetting = Disabled): MapConfigOptions =
    MapConfigOptions(dynamicTypeMapping, identityTypeMapping)
