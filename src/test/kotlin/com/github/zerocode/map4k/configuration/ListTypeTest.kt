package com.github.zerocode.map4k.configuration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.primaryConstructor

class ListTypeTest {

    interface Target

    @Test
    fun `correctly unwraps nested lists`() {
        data class TargetImpl(val ids: List<ArrayList<List<Target>>>)

        val actual = TypeDescriptor.resolve(
            TargetImpl::ids.returnTypeClass,
            TargetImpl::class.primaryConstructor!!.findParameterByName("ids")!!.type
        )

        val expected = ListDescriptor(
            List::class,
            ListDescriptor(
                ArrayList::class,
                ListDescriptor(
                    List::class,
                    AbstractDescriptor(Target::class)
                )
            )
        ) as TypeDescriptor

        assertThat(actual, equalTo(expected))
    }
}