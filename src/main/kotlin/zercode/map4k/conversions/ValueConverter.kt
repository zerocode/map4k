package zercode.map4k.conversions

import kotlin.reflect.KClass

interface ValueConverter<in TSource : Any, TTarget : Any> {
    fun canConvert(source: KClass<*>, target: KClass<*>): Boolean
    fun convert(source: TSource, target: KClass<*>): TTarget
}