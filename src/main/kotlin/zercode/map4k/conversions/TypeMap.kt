package zercode.map4k.conversions

import kotlin.reflect.KClass

interface TypeMap<TTarget : Any> {
    fun getMappedType(source: Any): KClass<TTarget>
}