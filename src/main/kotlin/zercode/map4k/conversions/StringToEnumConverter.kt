package zercode.map4k.conversions

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class StringToEnumConverter : ValueConverter<String, Enum<*>> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(String::class) && target.isSubclassOf(Enum::class)

    override fun convert(source: String, target: KClass<*>): Enum<*> =
        target.java.enumConstants!!.first { it.toString() == source } as Enum<*>
}