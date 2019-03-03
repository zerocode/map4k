package com.github.zerocode.map4k.configuration

import com.github.zerocode.map4k.extensions.returnTypeClass
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import kotlin.reflect.full.primaryConstructor

class MappingConfigTest {

    interface Source
    interface Target

    @Test
    fun `can retrieve a TypeMap for an abstract target`() {
        data class SourceImpl(val id: Int) : Source
        data class TargetImpl(val id: Int) : Target

        val config = config(typeMap<SourceImpl, TargetImpl>())
        val expected = config.typeMaps.first()
        val actual = config.typeMapFor(SourceImpl::class, Target::class)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can retrieve a TypeMap for an abstract source and target`() {
        data class SourceImpl(val id: Int) : Source
        data class TargetImpl(val id: Int) : Target

        val config = config(typeMap<SourceImpl, TargetImpl>())
        val expected = config.typeMaps.first()
        val actual = config.typeMapFor(Source::class, Target::class)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `creates an identity type map`() {
        data class SourceImpl(val id: Int) : Source

        val config = config(mappingOptions = options(dynamicTypeMapping = Enabled, identityTypeMapping = Enabled))
        assertThat(config.typeMaps, isEmpty)
        val actual = config.typeMapFor(SourceImpl::class, Source::class)
        assertThat(actual, present())
    }

    @Test
    fun `PropertyMap exists where target property is matched in source by name`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val expected = PropertyMap(
            targetProperty = Target::id,
            targetParameter = Target::class.primaryConstructor?.parameters?.first { it.name == "id" }!!,
            sourceResolution = NamedSourceResolution(sourceProperty = Source::id),
            conversion = TypeConversions.noopConverter(Source::id.returnTypeClass, Source::id.returnType, Target::id.returnTypeClass)
        )
        val actual = typeMap.propertyMaps.first()

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target property is matched in source by custom mapping`() {
        data class Source(val id: Int)
        data class Target(val otherId: Int)

        val expected = PropertyMap(
            targetProperty = Target::otherId,
            targetParameter = Target::class.primaryConstructor?.parameters?.first { it.name == "otherId" }!!,
            sourceResolution = NamedSourceResolution(sourceProperty = Source::id),
            conversion = TypeConversions.noopConverter(Source::id.returnTypeClass, Source::id.returnType, Target::otherId.returnTypeClass)
        )
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::otherId).build()
        val actual = typeMap.propertyMaps.first()

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target property is matched in source by custom mapping with conversion`() {
        data class Source(val id: Int)
        data class Target(val id: String)

        val expected = PropertyMap(
            targetProperty = Target::id,
            targetParameter = Target::class.primaryConstructor?.parameters?.first { it.name == "id" }!!,
            sourceResolution = ConvertedSourceResolution(sourceProperty = Source::id),
            conversion = SimpleTypeConverter(Source::id.returnTypeClass, Target::id.returnTypeClass, Int::toString as (Any) -> Any)
        )
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString).build()
        val actual = typeMap.propertyMaps.first()

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target has optional property not matched in source`() {
        data class Source(val id: Int)
        data class Target(val id: String, val name: String = "default_name")

        val expected = PropertyMap(
            targetProperty = Target::id,
            targetParameter = Target::class.primaryConstructor?.parameters?.first { it.name == "id" }!!,
            sourceResolution = ConvertedSourceResolution(sourceProperty = Source::id),
            conversion = SimpleTypeConverter(Source::id.returnTypeClass, Target::id.returnTypeClass, Int::toString as (Any) -> Any)
        )
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString).build()
        val actual = typeMap.propertyMaps.first()

        assertThat(actual, equalTo(expected))
    }
}