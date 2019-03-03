package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.Enabled
import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.options
import com.github.zerocode.map4k.configuration.typeConverter
import com.github.zerocode.map4k.configuration.typeConverters
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.junit.jupiter.api.Test

class CanMapMapTests {

    @Test
    fun `can map a subclass of List with closed type parameter`() {
        data class Source(val items: List<String>)
        data class TargetCustom(val items: StringList)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled),
                userDefinedTypeConversions = typeConverters(
                    typeConverter<List<String>, StringList> { x -> StringList(x) }
                )
            )
        )

        val source = Source(listOf("1", "2"))
        val target = TargetCustom(StringList(listOf("1", "2")))

        val actual = mapper.map<TargetCustom>(source)
        assertThat(actual.items, isA<StringList>())
        assertThat(actual.items, Matcher(List<String>::containsAll, target.items))
    }

    @Test
    fun `can map a map containing null values`() {
        data class Source(val items: Map<String, Any?>)
        data class Target(val items: Map<String, Any?>)

        val mapper = Mapper(
            config(
                mappingOptions = options(dynamicTypeMapping = Enabled)
            )
        )

        val source = Source(mapOf("1" to null))
        val expected = Target(mapOf("1" to null))

        val actual = mapper.map<Target>(source)
        assertThat(actual, equalTo(expected))
    }
}

class StringList(private val wrapped: List<String>) : List<String> {
    override val size: Int
        get() = wrapped.size

    override fun contains(element: String): Boolean {
        return wrapped.contains(element)
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        return wrapped.containsAll(elements)
    }

    override fun get(index: Int): String {
        return wrapped.get(index)
    }

    override fun indexOf(element: String): Int {
        return wrapped.indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return wrapped.isEmpty()
    }

    override fun iterator(): Iterator<String> {
        return wrapped.iterator()
    }

    override fun lastIndexOf(element: String): Int {
        return wrapped.lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<String> {
        return wrapped.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<String> {
        return wrapped.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<String> {
        return wrapped.subList(fromIndex, toIndex)
    }
}