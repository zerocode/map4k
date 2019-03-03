package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.Enabled
import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.options
import com.github.zerocode.map4k.configuration.typeConverter
import com.github.zerocode.map4k.configuration.typeConverters
import com.github.zerocode.map4k.configuration.typeMap
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KClass

class MapperTest {

    @Test
    fun `can map to the same type`() {
        data class Source(val id: Int)

        val mapper = Mapper(config(mappingOptions = options(identityTypeMapping = Enabled)))
        val actual = mapper.map<Source>(Source(1234))
        val expected = Source(1234)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with same names`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val mapper = Mapper(config(typeMap<Source, Target>()))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target(1234)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with different names`() {
        data class Source(val id: Int)
        data class Target(val otherId: Int)

        val mapper = Mapper(config(typeMap<Source, Target>().propertyMap(Source::id, Target::otherId)))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target(1234)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with different types`() {
        data class Source(val id: Int)
        data class Target(val id: String)

        val mapper = Mapper(config(typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString)))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target("1234")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with different types using a global converter`() {
        data class Source(val id: Int)
        data class Target(val id: String)

        val mapper = Mapper(config(
            typeMap<Source, Target>().propertyMap(Source::id, Target::id),
            userDefinedTypeConversions = typeConverters(
                typeConverter<Int, String> { value -> value.toString() }
            )
        ))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target("1234")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with different names and different types`() {
        data class Source(val id: Int)
        data class Target(val otherId: String)

        val mapper = Mapper(config(typeMap<Source, Target>().propertyMap(Source::id, Target::otherId, Int::toString)))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target("1234")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map to class with an optional property`() {
        data class Source(val id: String)
        data class Target(val id: String, val amount: Double = 105.7)

        val mapper = Mapper(config(typeMap<Source, Target>()))
        val actual = mapper.map<Target>(Source("some_id"))
        val expected = Target("some_id")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map to class with a non-constructor property`() {
        data class Source(val id: String) {
            val amount: Double = 0.0
        }
        data class Target(val id: String) {
            val amount: Double = 0.0
        }

        val mapper = Mapper(config(mappingOptions = options(dynamicTypeMapping = Enabled)))
        val actual = mapper.map<Target>(Source("some_id"))
        val expected = Target("some_id")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with nullable types`() {
        data class Source(val id: Int?)
        data class Target(val id: Int?)

        val mapper = Mapper(config(typeMap<Source, Target>()))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target(1234)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with null values`() {
        data class Source(val id: Int?)
        data class Target(val id: Int?)

        val mapper = Mapper(config(typeMap<Source, Target>()))
        val actual = mapper.map<Target>(Source(null))
        val expected = Target(null)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map properties with different nullable types`() {
        data class Source(val id: Int)
        data class Target(val id: String?)

        val mapper = Mapper(config(typeMap<Source, Target>().propertyMap(Source::id, Target::id, Int::toString)))
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target("1234")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map target property with derived value`() {
        data class Source(val id: Int)
        data class Target(val otherId: String)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>()
                    .propertyMap<Source, Target, String>(Target::otherId, { it.id.toString() + "5678" })
            )
        )
        val actual = mapper.map<Target>(Source(1234))
        val expected = Target("12345678")

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested data class`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild)
        data class TargetChild(val id: Int)
        data class Target(val child: TargetChild)

        val mapper = Mapper(config(typeMap<Source, Target>(), typeMap<SourceChild, TargetChild>()))
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(TargetChild(1234))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested data class with property conversion`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild)
        data class TargetChild(val otherId: String)
        data class Target(val child: TargetChild)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>(),
                typeMap<SourceChild, TargetChild>()
                    .propertyMap(SourceChild::id, TargetChild::otherId, Int::toString)
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(TargetChild("1234"))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested data class with named property map`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild)
        data class TargetChild(val otherId: String)
        data class Target(val otherChild: TargetChild)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>()
                    .propertyMap(Source::child, Target::otherChild),
                typeMap<SourceChild, TargetChild>()
                    .propertyMap(SourceChild::id, TargetChild::otherId, Int::toString)
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(TargetChild("1234"))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map a list of integers property`() {
        data class Source(val ids: List<Int>)
        data class Target(val ids: List<Int>)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>()
            )
        )
        val actual = mapper.map<Target>(Source(listOf(1, 2, 3)))
        val expected = Target(listOf(1, 2, 3))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map a nested data class with list of integers property`() {
        data class SourceChild(val ids: List<Int>)
        data class Source(val child: SourceChild)
        data class TargetChild(val ids: List<Int>)
        data class Target(val child: TargetChild)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>(),
                typeMap<SourceChild, TargetChild>()
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(listOf(1, 2, 3))))
        val expected = Target(TargetChild(listOf(1, 2, 3)))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map a nested data class with list of integers property and a conversion`() {
        data class SourceChild(val sourceIds: List<Int>)
        data class Source(val child: SourceChild)
        data class TargetChild(val targetIds: List<String>)
        data class Target(val child: TargetChild)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>(),
                typeMap<SourceChild, TargetChild>()
                    .propertyMap(
                        SourceChild::sourceIds,
                        TargetChild::targetIds,
                        { sourceIds -> sourceIds.map { it.toString() } }
                    )
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(listOf(1, 2, 3))))
        val expected = Target(TargetChild(listOf("1", "2", "3")))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested nullable data class`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild?)
        data class TargetChild(val id: Int)
        data class Target(val child: TargetChild?)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled)
            )
        )
        val actual = mapper.map<Target>(Source(null))
        val expected = Target(null)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested data class to primitive using conversion`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild)
        data class Target(val child: Int)

        val mapper = Mapper(
            config(
                typeMap<Source, Target>(),
                userDefinedTypeConversions = typeConverters(
                    typeConverter<SourceChild, Int> { sourceChild -> sourceChild.id }
                )
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(1234)

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map using dynamic type map`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild?)
        data class TargetChild(val id: Int)
        data class Target(val child: TargetChild?)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled)
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(TargetChild(1234))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map using dynamic type map with global conversions`() {
        data class SourceChild(val id: Int)
        data class Source(val child: SourceChild?)
        data class TargetChild(val id: String)
        data class Target(val child: TargetChild?)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled),
                userDefinedTypeConversions = typeConverters(
                    typeConverter(Int::toString)
                )
            )
        )
        val actual = mapper.map<Target>(Source(SourceChild(1234)))
        val expected = Target(TargetChild("1234"))

        assertThat(actual, equalTo(expected))
    }

    enum class Values { On, Off }

    @Test
    fun `can map using dynamic type map with global conversions and a list of enums`() {
        data class SourceChild(val enumValues: List<Values>)
        data class Source(val child: SourceChild?)
        data class TargetChild(val enumValues: List<String>)
        data class Target(val child: TargetChild?)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled),
                userDefinedTypeConversions = typeConverters(
                    typeConverter<String, Enum<*>> { value: Any, targetClass: KClass<*> -> targetClass.java.enumConstants!!.first { it.toString() == value } as Enum<*> },
                    typeConverter<Enum<*>, String> { value: Any -> value.toString() }
                )
            )
        )
        val source = Source(SourceChild(listOf(Values.On, Values.Off)))
        val target = Target(TargetChild(listOf("On", "Off")))

        val mappedTarget = mapper.map<Target>(source)
        val mappedSource = mapper.map<Source>(mappedTarget)

        assertThat(mappedTarget, equalTo(target))
        assertThat(mappedSource, equalTo(source))
    }

    @Test
    fun `can map using dynamic type map and a Map property`() {
        data class Source(val values: Map<String, Any>)
        data class Target(val values: Map<String, Any>)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled)
            )
        )
        val source = Source(mapOf("some_key" to 999))
        val target = Target(mapOf("some_key" to 999))

        val mappedTarget = mapper.map<Target>(source)

        assertThat(mappedTarget, equalTo(target))
    }

    data class SourceChild(val id: Int)
    data class TargetChild(val id: String)
    data class SourceWithMap(val values: List<Map<String, Map<String, SourceChild>>>)
    data class TargetWithMap(val values: List<Map<String, Map<String, TargetChild>>>)

    @Test
    fun `can map using dynamic type map and a property of nested maps`() {
        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled),
                userDefinedTypeConversions = typeConverters(
                    typeConverter(Int::toString)
                )
            )
        )
        val source = SourceWithMap(listOf(mapOf("some_key" to mapOf("some_nested_key" to SourceChild(1234)))))
        val target = TargetWithMap(listOf(mapOf("some_key" to mapOf("some_nested_key" to TargetChild("1234")))))

        val mappedTarget = mapper.map<TargetWithMap>(source)

        assertThat(mappedTarget, equalTo(target))
    }

    @Test
    // TODO - fix conversion bug
    fun `can map BSON`() {

        data class Source(val values: Document)
        data class Target(val values: Map<String, Any>)
        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled),
                userDefinedTypeConversions = typeConverters(
                    //typeConverter<Document, Map<String, Any>> { x -> x.toMap() }
                )
            ))
        val source = Source(Document(mapOf("key" to 999, "key2" to "")))
        val actual = mapper.map<Target>(source)
        assertThat(actual, equalTo(Target(mapOf("key" to 999, "key2" to ""))))
    }

    @Test
    fun `throws where no type map found`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val mapper = Mapper(config())

        assertThrows<MappingException> {
            mapper.map<Target>(Source(1234))
        }
    }
}