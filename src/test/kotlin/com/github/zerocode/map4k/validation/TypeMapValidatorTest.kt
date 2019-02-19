package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.configuration.InvalidConfigException
import com.github.zerocode.map4k.configuration.typeMap
import org.junit.jupiter.api.Disabled
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
    fun `does not throw where source property mapped by name`() {
        data class Source(val name: String)
        data class Target(val name: String)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `does not throw where source property mapped by custom`() {
        data class Source(val name: String)
        data class Target(val alternativeName: String)

        val typeMap = typeMap<Source, Target>().propertyMap(Source::name, Target::alternativeName).build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `does not throw where source is custom resolver`() {
        data class Source(val name: String)
        data class Target(val alternativeName: String)

        val typeMap = typeMap<Source, Target>().propertyMap<Source, Target, String>(Target::alternativeName, { x -> x.name }).build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    fun `does not throw where target property is optional`() {
        data class Source(val name: String)
        data class Target(val id: Int = 0)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertAll({ validator.validate() })
    }

    @Test
    @Disabled
    fun `throws where Source and Target property types are incompatible`() {
        data class Source(val id: String)
        data class Target(val id: Int)

        val typeMap = typeMap<Source, Target>().build()
        val validator = TypeMapValidator(typeMap)

        assertThrows<InvalidConfigException> { validator.validate() }
    }
}