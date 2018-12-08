package com.github.pseudometa.map4k.model

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf

interface Source {
    val kClass: KClass<out Any>
    val value: Any
}

data class BasicSource(
    override val kClass: KClass<out Any>,
    override val value: Any
) : Source

data class DataClassSource(
    override val kClass: KClass<out Any>,
    override val value: Any,
    val properties: List<PropertyModel>
) : Source {
    val simpleName: String = kClass.simpleName!!
    val propertyNames: List<String> = properties.map { it.name }
}

data class PropertyModel(
    val name: String,
    val source: Source
)

data class CollectionSource(
    override val kClass: KClass<out Any>,
    override val value: Any,
    val sources: List<Source>
) : Source

fun createSource(source: Any): Source = createSource(source, source::class)

fun createSource(source: Any, sourceKClass: KClass<*>): Source {
    return when {
        sourceKClass.isData -> DataClassSource(
            kClass = sourceKClass,
            value = source,
            properties = sourceKClass.declaredMemberProperties
                .filter { it.visibility!! == KVisibility.PUBLIC }
                .map { propertyModel(it, source) }
        )
        sourceKClass.isSubclassOf(List::class) -> CollectionSource(
            kClass = List::class,
            value = source,
            sources = (source as List<*>).map { createSource(it!!, it::class) }
        )
        sourceKClass.isAbstract -> createSource(source, source::class)
        else -> BasicSource(sourceKClass, source)
    }
}

fun propertyModel(kProperty: KProperty<Any?>, source: Any): PropertyModel {
    try {
        return PropertyModel(
            name = kProperty.name,
            source = createSource(kProperty.call(source)!!, kProperty.returnType.classifier as KClass<*>)
        )
    } catch (e: Exception) {
        println(kProperty)
        println(source)
        throw e
    }
}

