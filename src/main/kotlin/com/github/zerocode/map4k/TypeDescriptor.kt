package com.github.zerocode.map4k

import com.github.zerocode.map4k.extensions.firstTypeArgument
import com.github.zerocode.map4k.extensions.kClass
import com.github.zerocode.map4k.extensions.secondTypeArgument
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

sealed class TypeDescriptor(open val kClass: KClass<out Any>)
data class DataClassDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class ObjectDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass) { val instance = kClass.objectInstance }
data class PrimitiveDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class AbstractDescriptor(override val kClass: KClass<out Any>) : TypeDescriptor(kClass)
data class ListDescriptor(override val kClass: KClass<out Any>, val typeParameter: TypeDescriptor) : TypeDescriptor(kClass)
data class MapDescriptor(override val kClass: KClass<out Any>, val typeParameter: TypeDescriptor) : TypeDescriptor(kClass)
data class ArrayDescriptor(override val kClass: KClass<out Any>, val typeParameter: TypeDescriptor) : TypeDescriptor(kClass)

fun typeDescriptor(kClass: KClass<*>, typeParameter: KType? = null): TypeDescriptor = when {
    kClass.isData -> DataClassDescriptor(kClass)
    kClass.objectInstance != null -> ObjectDescriptor(kClass)
    kClass.java.isArray -> ArrayDescriptor(kClass, listParameterTypeDescriptor(kClass, typeParameter!!))
    kClass.isSubclassOf(List::class) -> ListDescriptor(kClass, listParameterTypeDescriptor(kClass, typeParameter!!))
    kClass.isSubclassOf(Map::class) -> MapDescriptor(kClass, mapParameterTypeDescriptor(kClass, typeParameter!!))
    kClass.isAbstract -> AbstractDescriptor(kClass)
    else -> PrimitiveDescriptor(kClass)
}

fun listParameterTypeDescriptor(targetClass: KClass<*>, targetParameterType: KType): TypeDescriptor {
    if (targetParameterType.arguments.isEmpty()) {
        val listType = targetClass.supertypes.firstOrNull { it.classifier as KClass<*> == List::class }
                       ?: throw IllegalArgumentException("List type not found in supertypes for target class $targetClass.")
        return typeDescriptor(listType.firstTypeArgument.kClass, listType.firstTypeArgument)
    }
    if (targetParameterType.arguments.size == 1) {
        return typeDescriptor(targetParameterType.firstTypeArgument.kClass, targetParameterType.firstTypeArgument)
    } else {
        throw IllegalArgumentException("Unable to resolve type parameter for $targetClass.")
    }
}

fun mapParameterTypeDescriptor(targetClass: KClass<*>, targetParameterType: KType): TypeDescriptor {
    if (targetParameterType.arguments.isEmpty()) {
        val mapType = targetClass.supertypes.firstOrNull { it.classifier as KClass<*> == Map::class }
                      ?: throw IllegalArgumentException("Map type not found in supertypes for target class $targetClass.")
        return typeDescriptor(mapType.secondTypeArgument.kClass, mapType.secondTypeArgument)
    }
    if (targetParameterType.arguments.size == 2) {
        return typeDescriptor(targetParameterType.secondTypeArgument.kClass, targetParameterType.secondTypeArgument)
    } else {
        throw IllegalArgumentException("Unable to resolve type parameter for $targetClass.")
    }
}
