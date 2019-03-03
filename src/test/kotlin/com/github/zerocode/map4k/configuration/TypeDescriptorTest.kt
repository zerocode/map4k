package com.github.zerocode.map4k.configuration

import com.github.zerocode.map4k.AbstractDescriptor
import com.github.zerocode.map4k.ListDescriptor
import com.github.zerocode.map4k.MapDescriptor
import com.github.zerocode.map4k.ObjectDescriptor
import com.github.zerocode.map4k.TypeDescriptor
import com.github.zerocode.map4k.extensions.returnTypeClass
import com.github.zerocode.map4k.typeDescriptor
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.primaryConstructor

class TypeDescriptorTest {

    interface Target

    @Test
    fun `correctly unwraps nested lists`() {
        data class TargetImpl(val ids: List<ArrayList<List<Target>>>)

        val actual = typeDescriptor(
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

    @Test
    fun `creates descriptor for map`() {
        data class TargetImpl(val ids: Map<String, Target>)

        val actual = typeDescriptor(
            TargetImpl::ids.returnTypeClass,
            TargetImpl::class.primaryConstructor!!.findParameterByName("ids")!!.type
        )

        val expected = MapDescriptor(
            Map::class, AbstractDescriptor(Target::class)
        ) as TypeDescriptor

        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `creates descriptor for nested maps`() {
        data class TargetImpl(val ids: List<Map<String, Map<String, List<Target>>>>)

        val actual = typeDescriptor(
            TargetImpl::ids.returnTypeClass,
            TargetImpl::class.primaryConstructor!!.findParameterByName("ids")!!.type
        )

        val expected = ListDescriptor(
            List::class,
            MapDescriptor(
                Map::class,
                MapDescriptor(
                    Map::class,
                    ListDescriptor(
                        List::class,
                        AbstractDescriptor(Target::class)
                    )
                )
            )
        ) as TypeDescriptor

        assertThat(actual, equalTo(expected))
    }

    object SomeObject

    @Test
    fun `creates descriptor for an object`() {
        data class TargetImpl(val someObject: SomeObject)

        val actual = typeDescriptor(
            TargetImpl::someObject.returnTypeClass,
            TargetImpl::class.primaryConstructor!!.findParameterByName("someObject")!!.type
        )

        val expected = ObjectDescriptor(SomeObject::class) as TypeDescriptor

        assertThat(actual, equalTo(expected))
    }
}