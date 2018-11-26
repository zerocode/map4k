package zercode.map4k.conversions

import kotlin.reflect.KClass

class TypeMapRegistry(vararg typeMaps: TypeMap<*>) {
    private val maps = typeMaps.map { key(it) to it }.toMap()

    fun getMap(target: KClass<*>): TypeMap<*> =
        maps[target]
            ?: throw TypeMappingException("TypeMap not found for $target. A TypeMap is required when mapping to abstract types.")

    private fun key(typeMap: TypeMap<*>): KClass<*> =
        typeMap::class.supertypes.first().arguments[0].type!!.classifier as KClass<*>
}