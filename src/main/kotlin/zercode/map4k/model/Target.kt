package zercode.map4k.model

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

interface Target {
    val kClass: KClass<*>
}

data class BasicTarget(override val kClass: KClass<*>) : Target

data class DataClassTarget(
    override val kClass: KClass<*>,
    val parameters: List<ParameterTarget>
) : Target {
    val simpleName: String = kClass.simpleName!!
    val parameterNames: List<String> = parameters.map { it.name }
}

data class AbstractTarget(
    override val kClass: KClass<*>
) : Target

data class ParameterTarget(
    override val kClass: KClass<*>,
    val name: String,
    val kParameter: KParameter,
    val target: Target
) : Target

data class CollectionTarget(
    override val kClass: KClass<*>,
    val containedTypeTarget: Target
) : Target

fun createTarget(kClass: KClass<*>): Target = when {
    kClass.isData -> dataClassTarget(kClass)
    kClass.isSubclassOf(List::class) -> CollectionTarget(kClass, createTarget(kClass.typeParameters.first().upperBounds.first()))
    kClass.isAbstract -> AbstractTarget(kClass)
    else -> BasicTarget(kClass)
}

fun createTarget(kType: KType): Target {
    val kClass = kType.classifier as KClass<*>
    return when {
        kClass.isSubclassOf(List::class) -> CollectionTarget(kClass, containedTypeTarget(kType))
        else -> createTarget(kClass)
    }
}

private fun dataClassTarget(kClass: KClass<*>): DataClassTarget =
    DataClassTarget(
        kClass = kClass,
        parameters = kClass.primaryConstructor!!.parameters.map { parameterTarget(it) }
    )

fun parameterTarget(kParameter: KParameter): ParameterTarget =
    ParameterTarget(
        kClass = kParameter.type.classifier as KClass<*>,
        name = kParameter.name!!,
        kParameter = kParameter,
        target = createTarget(kParameter.type)
    )

fun containedTypeTarget(type: KType): Target {
    val kClass = type.arguments.first().type!!.classifier as KClass<*>
    return when {
        kClass.isSubclassOf(List::class) -> createTarget(type.arguments.first().type!!)
        else -> createTarget(kClass)
    }
}