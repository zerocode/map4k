package com.github.zerocode.map4k.validation

import com.github.zerocode.map4k.configuration.config
import com.github.zerocode.map4k.configuration.typeMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapConfigValidatorTest {

    @Test
    fun `throws where config contains duplicate type maps`() {
        data class Source(val id: Int)
        data class Target(val id: Int)

        val config = config(typeMap<Source, Target>(), typeMap<Source, Target>())
        val validator = MapConfigValidator(config)

        assertThrows<DuplicateTypeMapException> { validator.validate() }
    }
}