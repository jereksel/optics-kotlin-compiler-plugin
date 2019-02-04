package com.ivianuu.debuglog

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@AutoService(CommandLineProcessor::class)
class MyCommandLineProcessor : CommandLineProcessor {
    /**
     * Just needs to be consistent with the key for DebugLogGradleSubplugin#getCompilerPluginId
     */
    override val pluginId: String = "com.ivianuu.debuglog"

    /**
     * Should match up with the options we return from our DebugLogGradleSubplugin.
     * Should also have matching when branches for each name in the [processOption] function below
     */
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = "enabled", valueDescription = "<true|false>",
            description = "whether to enable the debuglog plugin or not"
        ),
        CliOption(
            optionName = "debugLogAnnotation", valueDescription = "<fqname>",
            description = "fully qualified name of the annotation(s) to use as debug-log",
            required = true,
            allowMultipleOccurrences = true
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
            "debugLogAnnotation" -> configuration.appendList(KEY_ANNOTATIONS, value)
            else -> error("Unexpected config option ${option.optionName}")
        }
    }

}

val KEY_ENABLED = CompilerConfigurationKey<Boolean>("whether the plugin is enabled")
val KEY_ANNOTATIONS = CompilerConfigurationKey<List<String>>("our debuglog annotations")
