package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class MapConfigTest {

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
    fun `PropertyMap exists where target property is matched in source by name`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val expected = listOf(namedPropertyMap(Source::id, Target::id)) as Collection<PropertyMap>
        val actual = typeMap.propertyMaps

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target property is matched in source by custom mapping`() {
        data class Source(val id: Int)
        data class Target(val otherId: Int)

        val expected = namedPropertyMap(Source::id, Target::otherId)
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::otherId).build()
        val actual = typeMap.propertyMaps.first()

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target property is matched in source by custom mapping with conversion`() {
        data class Source(val id: Int)
        data class Target(val id: String)

        val expected = listOf(convertedPropertyMap(Source::id, Target::id, Int::toString)) as Collection<PropertyMap>
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString).build()
        val actual = typeMap.propertyMaps

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `PropertyMap exists where target has optional property not matched in source`() {
        data class Source(val id: Int)
        data class Target(val id: String, val name: String = "default_name")

        val expected = listOf(convertedPropertyMap(Source::id, Target::id, Int::toString)) as Collection<PropertyMap>
        val typeMap = typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString).build()
        val actual = typeMap.propertyMaps

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun checker() {
        val animal = Animal("lion")
        check(animal)

        val giraffe = Giraffe(10)
        check(giraffe)

        val organism = Giraffe(10) as Organism
        check(organism)

        check<Animal>(giraffe)
        check<Organism>(giraffe)
    }
}

interface Organism
open class Animal(open val name: String) : Organism
data class Giraffe(val height: Int) : Animal("Giraffe")

inline fun <reified T : Any> check(item: T) {
    println("Checking type: ${T::class}")
    println("Checking item: ${item::class}")
}