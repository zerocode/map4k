package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.typeConverter
import com.github.zerocode.map4k.configuration.typeConverters
import com.github.zerocode.map4k.configuration.typeMap
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class CanMapListsTest {

    data class SourceChild(val id: Int)
    data class SourceParent(val children: List<SourceChild>)
    data class TargetChild(val id: Int)
    data class TargetParent(val children: List<TargetChild>)

    interface Source {
        val id: Int
    }

    interface Target {
        val id: Int
    }

    data class SourceImplA(override val id: Int = 1234) : Source
    data class SourceImplB(override val id: Int = 1234, val name: String = "don pablo") : Source
    data class TargetImplA(override val id: Int, val name: String) : Target
    data class TargetImplC(override val id: Int, val otherId: String) : Target

    @Test
    fun `can map a nested list of data classes`() {
        val mapper = Mapper(
            config(
                typeMap<SourceParent, TargetParent>(),
                typeMap<SourceChild, TargetChild>()
            )
        )
        val actual = mapper.map<TargetParent>(SourceParent(listOf(SourceChild(1234))))
        val expected = TargetParent(listOf(TargetChild(1234)))

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested lists of primitive types with a conversion`() {
        data class SourceItems(val items: List<List<Int>> = listOf(listOf(1, 2)))
        data class TargetItems(val items: List<List<String>>)

        val source = SourceItems()
        val expected = TargetItems(items = listOf(listOf("1", "2")))

        val mapper = Mapper(
            config(
                typeMap<SourceItems, TargetItems>(),
                typeConversions = typeConverters(
                    typeConverter(Int::toString)
                )
            )
        )

        val actual = mapper.map<TargetItems>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested lists of lists of abstract types`() {
        data class SourceItems(val items: List<List<Source>> = listOf(listOf(SourceImplA(), SourceImplB())))
        data class TargetItems(val items: List<List<Target>>)

        val source = SourceItems()
        val expected = TargetItems(items = listOf(
            listOf(
                TargetImplC(id = 1234, otherId = "1234"),
                TargetImplA(id = 1234, name = "don pablo")
            )))

        val mapper = Mapper(
            config(
                typeMap<SourceItems, TargetItems>(),
                typeMap<SourceImplA, TargetImplC>()
                    .propertyMap(SourceImplA::id, TargetImplC::otherId, Int::toString),
                typeMap<SourceImplB, TargetImplA>()
            )
        )

        val actual = mapper.map<TargetItems>(source)
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map nested lists of lists of abstract types with a conversion from list to arraylist type`() {
        data class SourceItems(val items: List<List<Source>> = listOf(listOf(SourceImplA(), SourceImplB())))
        data class TargetItems(val items: List<ArrayList<Target>>)

        val mapper = Mapper(
            config(
                typeMap<SourceItems, TargetItems>(),
                typeMap<SourceImplA, TargetImplC>()
                    .propertyMap(SourceImplA::id, TargetImplC::otherId, Int::toString),
                typeMap<SourceImplB, TargetImplA>()
            )
        )

        val expected = TargetItems(items = listOf(
            arrayListOf(
                TargetImplC(id = 1234, otherId = "1234"),
                TargetImplA(id = 1234, name = "don pablo")
            )))

        val actual = mapper.map<TargetItems>(SourceItems())
        assertThat(actual, equalTo(expected))
    }

/*    @Test
    fun `can map a list of primitive types with a conversion`() {
        val source = listOf(1, 2, 3)

        val mapper = Mapper(
            config(
                typeConversions = typeConverters(
                    typeConverter<Int, String> { it.toString() }
                )
            )
        )

        val expected = listOf("1", "2", "3")
        val actual = mapper.map<List<String>>(source)
        assertThat(actual, equalTo(expected))
    }*/
}