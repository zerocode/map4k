package com.github.pseudometa.map4k.conversions

import org.bson.types.ObjectId
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class StringToObjectIdConverter : ValueConverter<String, ObjectId> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(String::class) && target.isSubclassOf(ObjectId::class)

    override fun convert(source: String, target: KClass<*>): ObjectId =
        ObjectId(source)
}