package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("vnext2")
class TypeConversionsBuilderTest {

    private val conversions = typeConverters(
        typeConverter<String, Int> { it.toInt() },
        typeConverter<Int, String> { it.toString() }
    )

    @Test
    fun `can convert string to int`() {
        val actual = conversions.convert<String, Int>("60")
        assertThat(actual, equalTo(60))
    }

    @Test
    fun `can convert int to string`() {
        val actual = conversions.convert<Int, String>(60)
        assertThat(actual, equalTo("60"))
    }
}