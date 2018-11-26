package zercode.map4k.conversions

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ListToArrayListConverter : ValueConverter<List<*>, ArrayList<*>> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(List::class) && target.isSubclassOf(ArrayList::class)

    override fun convert(source: List<*>, target: KClass<*>): ArrayList<*> =
        arrayListOf(*source.toTypedArray())
}