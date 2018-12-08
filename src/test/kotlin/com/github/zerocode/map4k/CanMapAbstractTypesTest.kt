package com.github.zerocode.map4k

import com.github.zerocode.map4k.configuration.Enabled
import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.options
import com.github.zerocode.map4k.configuration.typeMap
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("vnext2")
class CanMapAbstractTypesTest {

    interface Source {
        val id: Int
    }

    interface Target {
        val id: Int
    }

    data class SourceImplA(override val id: Int = 1234) : Source
    data class SourceImplB(override val id: Int = 1234, val name: String = "don pablo") : Source
    data class SourceImplC(override val id: Int = 1234, val amount: Double = 99.9) : Source
    data class TargetImplA(override val id: Int, val name: String) : Target
    data class TargetImplB(override val id: Int, val amount: Double) : Target
    data class TargetImplC(override val id: Int, val otherId: String) : Target

    @Test
    fun `can map abstract types`() {
        val mapper = MapperV3(
            config(
                typeMap<SourceImplB, TargetImplA>()
            )
        )
        val actual = mapper.map<Target>(SourceImplB())
        val expected = TargetImplA(1234, "don pablo") as Target

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map the same abstract type properties`() {
        data class ParentSource(val inner: Source)
        data class ParentTarget(val inner: Source)

        val mapper = MapperV3(
            config(
                typeMap<ParentSource, ParentTarget>(),
                options = options(identityTypeMapping = Enabled)
            )
        )
        val actual = mapper.map<ParentTarget>(ParentSource(SourceImplB()))
        val expected = ParentTarget(SourceImplB())

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `can map different abstract type properties`() {
        data class ParentSource(val inner: Source)
        data class ParentTarget(val inner: Target)

        val mapper = MapperV3(
            config(
                typeMap<ParentSource, ParentTarget>(),
                typeMap<SourceImplB, TargetImplA>()
            )
        )
        val actual = mapper.map<ParentTarget>(ParentSource(SourceImplB()))
        val expected = ParentTarget(TargetImplA(1234, "don pablo"))

        assertThat(actual, equalTo(expected))
    }

    // TODO - the same source type maps to more than one target type
    @Test
    fun `can map lists of abstract types`() {
        data class ParentSource(val inner: List<Source>)
        data class ParentTarget(val inner: List<Target>)

        val mapper = MapperV3(
            config(
                typeMap<ParentSource, ParentTarget>(),
                typeMap<SourceImplA, TargetImplC>()
                    .propertyMap(SourceImplA::id, TargetImplC::otherId, Int::toString),
                typeMap<SourceImplB, TargetImplA>(),
                typeMap<SourceImplC, TargetImplB>()
            )
        )
        val actual = mapper.map<ParentTarget>(
            ParentSource(
                listOf(
                    SourceImplA(),
                    SourceImplB(),
                    SourceImplC()
                )
            )
        )
        val expected = ParentTarget(
            listOf(
                TargetImplC(1234, "1234"),
                TargetImplA(1234, "don pablo"),
                TargetImplB(1234, 99.9)
            )
        )

        assertThat(actual, equalTo(expected))
    }
}