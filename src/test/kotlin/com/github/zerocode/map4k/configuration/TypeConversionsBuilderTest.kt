package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

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
}