package zercode.map4k.conversions

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class EnumToStringConverter : ValueConverter<Enum<*>, String> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(Enum::class) && target.isSubclassOf(String::class)

    override fun convert(source: Enum<*>, target: KClass<*>): String =
        source.toString()
}