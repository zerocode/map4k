package com.github.zerocode.map4k.configuration

sealed class OptionSetting
object Enabled : OptionSetting()
object Disabled : OptionSetting()

data class MappingOptions(
    val dynamicTypeMapping: OptionSetting = Disabled,
    val identityTypeMapping: OptionSetting = Disabled
)