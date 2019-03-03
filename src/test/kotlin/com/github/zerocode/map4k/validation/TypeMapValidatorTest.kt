package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.configuration.Enabled
import com.github.zerocode.map4k.configuration.InvalidConfigException
import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.options
import com.github.zerocode.map4k.configuration.typeConverter
import com.github.zerocode.map4k.configuration.typeConverters
import com.github.zerocode.map4k.configuration.typeMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class TypeMapValidatorTest {

    @Test
    fun `throws where Source property not found by name, custom mapping, value resolver and target parameter is not optional`() {
        data class Source(val name: String)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertThrows<InvalidConfigException> { validator.validate() }
    }

    @Test
    fun `validation succeeds where source property mapped by name`() {
        data class Source(val name: String)
        data class Target(val name: String)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `validation succeeds where source property resolved by named resolution`() {
        data class Source(val name: String)
        data class Target(val alternativeName: String)

        val typeMap = typeMap<Source, Target>().propertyMap(Source::name, Target::alternativeName).build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `validation succeeds where target property is optional`() {
        data class Source(val name: String)
        data class Target(val id: Int = 0)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `throws where Source and Target property primitive types are incompatible`() {
        data class Source(val id: String)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertThrows<InvalidConfigException> { validator.validate() }
    }

    @Test
    fun `validation succeeds where Source and Target property list parameter types are compatible`() {
        data class Source(val items: List<String>)
        data class Target(val items: List<String>)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `throws where Source and Target property list parameter types are incompatible`() {
        data class Source(val items: List<String>)
        data class Target(val items: List<Int>)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertThrows<InvalidConfigException> { validator.validate() }
    }

    @Test
    fun `validation succeeds where Source and Target property list parameter types are incompatible but a conversion exists`() {
        data class Source(val items: List<String>)
        data class Target(val items: List<Int>)

        val config = config(
            typeMap<Source, Target>(),
            userDefinedTypeConversions = typeConverters(
                typeConverter<String, Int> { x -> Integer.parseInt(x) }
            )
        )
        val validator = TypeMapValidator(config.typeMaps.first(), config)

        assertAll({ validator.validate() })
    }

    @Test
    fun `throws where Source and Target property nested list parameter types are incompatible`() {
        data class Source(val items: List<List<String>>)
        data class Target(val items: List<List<Int>>)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertThrows<InvalidConfigException> { validator.validate() }
    }

    interface Source {
        val id: Int
    }

    interface Target {
        val id: Int
    }

    object ObjectSource : Source {
        override val id: Int = 1
    }

    object ObjectTarget : Target {
        override val id: Int = 1
    }

    @Test
    fun `throws where dynamically created map for abstract target is invalid`() {
        data class ParentSource(val inner: Source)
        data class ParentTarget(val inner: Target)

        val config = config(
            typeMap<ParentSource, ParentTarget>(),
            mappingOptions = options(
                dynamicTypeMapping = Enabled
            )
        )

        val validator = TypeMapValidator(config.typeMaps.first())
        assertThrows<InvalidConfigException> { validator.validate() }
    }

    @Test
    fun `validation succeeds where source is resolved by derived resolution`() {
        data class Source(val name: String)
        data class Target(val alternativeName: String)

        val typeMap = typeMap<Source, Target>().propertyMap(Target::alternativeName, { x: Source -> x.name }).build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }
}