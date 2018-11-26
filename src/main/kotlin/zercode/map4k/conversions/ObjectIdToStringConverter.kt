package zercode.map4k.conversions

import org.bson.types.ObjectId
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ObjectIdToStringConverter : ValueConverter<ObjectId, String> {
    override fun canConvert(source: KClass<*>, target: KClass<*>): Boolean =
        source.isSubclassOf(ObjectId::class) && target.isSubclassOf(String::class)

    override fun convert(source: ObjectId, target: KClass<*>): String =
        source.toHexString()
}