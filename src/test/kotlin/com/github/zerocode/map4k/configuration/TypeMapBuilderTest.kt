package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class TypeMapBuilderTest {

    @Test
    fun `automatically generates a property map`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val actual = typeMap.propertyMaps
        val expected = listOf(PropertyMap(
            targetProperty = Target::id,
            targetParameter = Target::class.primaryConstructor!!.parameters.first(),
            sourceResolution = NamedSourceResolution(Source::id),
            conversion = TypeConversions.noopConverter(Int::class, Int::class)
        )) as Collection<PropertyMap>

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `automatically generates a property map and applies a type conversion`() {
        data class Source(val id: Int)
        data class Target(val id: String)

        val converter = { x: Any -> x.toString() }
        val config = config(
            typeMap<Source, Target>(),
            typeConversions = typeConverters(
                typeConverter<Int, String>(converter)
            )
        )
        val actual = config.typeMaps.first().propertyMaps
        val expected = listOf(PropertyMap(
            targetProperty = Target::id,
            targetParameter = Target::class.primaryConstructor!!.parameters.first(),
            sourceResolution = NamedSourceResolution(Source::id),
            conversion = SimpleTypeConverter(Int::class, String::class, converter)
        )) as Collection<PropertyMap>

        assertThat(actual, equalTo(expected))
    }
}