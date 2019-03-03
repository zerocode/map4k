package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.bson.Document
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

enum class SomeTestEnum {
    SomeEnumValue
}

class TypeConversionsTest {

    private val conversions = typeConverters(
        typeConverter<String, Enum<*>> { value: Any, targetClass: KClass<*> -> targetClass.java.enumConstants!!.first { it.toString() == value } as Enum<*> },
        typeConverter<Enum<*>, String> { value: Any -> value.toString() }
    )

    @Test
    fun `can convert string to enum`() {
        val source = "SomeEnumValue"
        val converter = conversions.getConverter(source::class, SomeTestEnum::class)!!
        val actual = converter.convert(source, SomeTestEnum::class) as SomeTestEnum
        assertThat(actual, equalTo(SomeTestEnum.SomeEnumValue))
    }

    @Test
    fun `can convert enum to string`() {
        val source = SomeTestEnum.SomeEnumValue
        val converter = conversions.getConverter(source::class, String::class)!!
        val actual = converter.convert(source, String::class) as String
        assertThat(actual, equalTo("SomeEnumValue"))
    }

    @Test
    fun `can convert BSON doc to map`() {
        val conversions = typeConverters(
            typeConverter<Document, Map<String, Any>> { x -> x.toMap() }
        )
        val source = Document(mapOf("key" to 999, "key2" to ""))
        val converter = conversions.getConverter(source::class, Map::class)!!
        val actual = converter.convert(source, Map::class) as Map<String, Any>
        assertThat(actual, equalTo(mapOf("key" to 999, "key2" to "")))
    }

    @Test
    @Disabled
    fun `can convert from List to Array`() {
        val conversions = typeConverters(
            typeConverter<List<*>, Array<*>> { x -> x.toTypedArray() }
        )
        val source = listOf("1", "2")
        val target = arrayOf("1", "2")
        val targetClass = target::class

        val tc = typeConverter<List<String>, Array<String>> { x -> x.toTypedArray() }
        val s = source::class.isSubclassOf(tc.sourceBaseClass)
        val t = target::class.isSubclassOf(tc.targetBaseClass)


        val converter = conversions.getConverter(source::class, target::class)!!
        val actual = converter.convert(source, target::class) as Array<String>
        assertThat(actual, equalTo(target))
    }
}