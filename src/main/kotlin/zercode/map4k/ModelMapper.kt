package zercode.map4k

import zercode.map4k.conversions.ArrayListToListConverter
import zercode.map4k.conversions.EnumToStringConverter
import zercode.map4k.conversions.ListToArrayListConverter
import zercode.map4k.conversions.ObjectIdToStringConverter
import zercode.map4k.conversions.StringToEnumConverter
import zercode.map4k.conversions.StringToObjectIdConverter
import zercode.map4k.conversions.TypeMapRegistry
import zercode.map4k.conversions.TypeMappingException
import zercode.map4k.conversions.ValueConverterRegistry
import zercode.map4k.extensions.joinWhere
import zercode.map4k.model.AbstractTarget
import zercode.map4k.model.BasicSource
import zercode.map4k.model.BasicTarget
import zercode.map4k.model.CollectionSource
import zercode.map4k.model.CollectionTarget
import zercode.map4k.model.DataClassSource
import zercode.map4k.model.DataClassTarget
import zercode.map4k.model.ParameterTarget
import zercode.map4k.model.PropertyModel
import zercode.map4k.model.Source
import zercode.map4k.model.Target
import zercode.map4k.model.createSource
import zercode.map4k.model.createTarget
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

val mapper = ModelMapper()

inline fun <reified TTarget : Any> mapTo(source: Any): TTarget =
    mapper.mapTo(source, TTarget::class) as TTarget

class ModelMapper(
    private val typeMapRegistry: TypeMapRegistry = TypeMapRegistry()
) {
    private val valueConverterRegistry = ValueConverterRegistry(
        ObjectIdToStringConverter(),
        StringToObjectIdConverter(),
        ListToArrayListConverter(),
        ArrayListToListConverter(),
        EnumToStringConverter(),
        StringToEnumConverter()
    )

    inline fun <reified TTarget : Any> mapTo(source: Any): TTarget =
        mapTo(source, TTarget::class) as TTarget

    fun mapTo(source: Any, target: KClass<out Any>): Any {
        val sourceModel = createSource(source)
        val targetModel = createTarget(target)
        return resolveValue(sourceModel, targetModel)
    }

    private fun resolveValue(source: Source, target: Target): Any =
        when {
            areTheSameOrRelatedTypes(source, target) -> source.value
            source is BasicSource && target is BasicTarget -> resolveValue(source, target)
            source is DataClassSource && target is DataClassTarget -> resolveValue(source, target)
            source is CollectionSource && target is CollectionTarget -> resolveValue(source, target)
            source is DataClassSource && target is AbstractTarget -> resolveValue(source, target)
            conversionAvailable(source, target) -> convertValue(source, target)
            else -> throw TypeMappingException("Cannot resolve value for ${source.kClass} and ${target.kClass}.")
        }

    private fun resolveValue(source: BasicSource, target: BasicTarget): Any {
        assertTypeCompatibility(source, target)
        return if (conversionAvailable(source, target)) convertValue(source, target)
        else source.value
    }

    private fun assertTypeCompatibility(source: BasicSource, target: BasicTarget) {
        if (!target.kClass.isSuperclassOf(source.kClass) && !conversionAvailable(source, target)) {
            throw TypeMappingException("Cannot convert $(source.kClass) to ${target.kClass}. Incompatible types and no conversion foound")
        }
    }

    private fun convertValue(source: Source, target: Target) =
        valueConverterRegistry.getConverter(source.kClass, target.kClass).convert(source.value, target.kClass)

    private fun conversionAvailable(source: Source, target: Target) =
        valueConverterRegistry.containsConverter(source.kClass, target.kClass)

    private fun resolveValue(source: CollectionSource, target: CollectionTarget): Any {
        val resolvedValues = source.sources.map { resolveValue(it, target.containedTypeTarget) }
        return if (conversionAvailable(source, target)) {
            valueConverterRegistry.getConverter(source.kClass, target.kClass).convert(resolvedValues, target.kClass)
        } else resolvedValues
    }

    private fun resolveValue(source: DataClassSource, target: AbstractTarget): Any =
        resolveValue(source, getTypeConversion(source, target))

    private fun getTypeConversion(source: Source, target: Target): Target =
        createTarget(typeMapRegistry.getMap(target.kClass).getMappedType(source.value))

    private fun resolveValue(source: DataClassSource, target: DataClassTarget): Any {
        assertCanMapBetween(source, target)
        return createInstance(target, targetConstructorParameters(source, target))
    }

    private fun createInstance(target: DataClassTarget, parameterValueMap: Map<KParameter, Any>) =
        target.kClass.primaryConstructor!!.callBy(parameterValueMap)

    private fun targetConstructorParameters(source: DataClassSource, target: DataClassTarget): Map<KParameter, Any> =
        source.properties
            .joinWhere(target.parameters) { property, parameter -> hasTheSameIdentifier(property, parameter) }
            .map { (property, parameter) -> parameter.kParameter to resolveValue(property.source, parameter.target) }.toMap()

    private fun hasTheSameIdentifier(sourceProperty: PropertyModel, targetParameter: ParameterTarget) =
        targetParameter.name == sourceProperty.name

    private fun areTheSameOrRelatedTypes(source: Source, target: Target) =
        source !is CollectionSource &&
            target !is CollectionTarget &&
            target.kClass.isSuperclassOf(source.kClass)

    private fun assertCanMapBetween(source: DataClassSource, target: DataClassTarget) {
        if (!hasMatchingProperties(source, target)) {
            throw TypeMappingException("Source [${source.simpleName}] cannot be mapped to target [${target.simpleName}]. " +
                "Cannot find source properties for all target properties [${missingSourceProperties(source, target).joinToString(", ")}")
        }
    }

    private fun hasMatchingProperties(source: DataClassSource, target: DataClassTarget): Boolean =
        target.parameterNames.any { source.propertyNames.contains(it) }

    private fun missingSourceProperties(source: DataClassSource, target: DataClassTarget): List<String> =
        target.parameterNames.filterNot { source.propertyNames.contains(it) }
}