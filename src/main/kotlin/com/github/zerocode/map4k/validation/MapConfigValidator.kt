package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.configuration.MapConfig
import com.github.zerocode.map4k.configuration.TypeMap

class MapConfigValidator(private val config: MapConfig) {

    fun validate() {
        assertNoDuplicates()
        config.typeMaps.forEach { validate(it) }
    }

    private fun assertNoDuplicates() {
        config.typeMaps
            .groupBy { it.sourceClass.java.typeName + it.targetClass.java.typeName }
            .forEach { if (it.value.size > 1) throw DuplicateTypeMapException("${it.value.first()}") }
    }

    private fun validate(typeMap: TypeMap) =
        TypeMapValidator(typeMap).validate()
}

class DuplicateTypeMapException(message: String) : Exception(message)