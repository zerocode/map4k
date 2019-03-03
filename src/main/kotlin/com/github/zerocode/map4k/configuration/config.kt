package com.github.zerocode.map4k.configuration

inline fun <reified TSource : Any, reified TTarget : Any> typeMap(): TypeMapBuilder {
    return TypeMapBuilder(TSource::class, TTarget::class)
}

fun config(
    vararg userDefinedTypeMapBuilders: TypeMapBuilder,
    userDefinedTypeConversions: TypeConversions = TypeConversions(),
    mappingOptions: MappingOptions = MappingOptions()
): MappingConfig {
    return MappingConfig(
        userDefinedTypeMapBuilders.map { it.build(userDefinedTypeConversions, mappingOptions) },
        userDefinedTypeConversions,
        mappingOptions
    )
}

fun options(
    dynamicTypeMapping: OptionSetting = Disabled,
    identityTypeMapping: OptionSetting = Disabled
): MappingOptions {
    return MappingOptions(dynamicTypeMapping, identityTypeMapping)
}