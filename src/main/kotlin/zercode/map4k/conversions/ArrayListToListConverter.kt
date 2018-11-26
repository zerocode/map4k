package zercode.map4k.conversions

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ArrayListToListConverter : ValueConverter<ArrayList<out Any>, List<Any>> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(ArrayList::class) && target.isSubclassOf(List::class)

    override fun convert(source: ArrayList<out Any>, target: KClass<*>): List<Any> =
        listOf(*source.toTypedArray())
}