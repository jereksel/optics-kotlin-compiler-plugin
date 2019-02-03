package com.ivianuu.debuglog

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class MyKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

    override fun isApplicable(project: Project, task: AbstractCompile) =
        project.plugins.hasPlugin(MyGradlePlugin::class.java)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        val extension = project.extensions.findByType(MyGradleExtension::class.java)
            ?: MyGradleExtension()

        if (extension.enabled && extension.annotations.isEmpty()) {
            error("DebugLog is enabled, but no annotations were set")
        }

        val annotationOptions =
            extension.annotations.map { SubpluginOption(key = "debugLogAnnotation", value = it) }
        val enabledOption = SubpluginOption(key = "enabled", value = extension.enabled.toString())
        return annotationOptions + enabledOption
    }

    /**
     * Just needs to be consistent with the key for DebugLogCommandLineProcessor#pluginId
     */
    override fun getCompilerPluginId(): String = "com.ivianuu.debuglog"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.ivianuu.debuglog",
        artifactId = "kotlin-plugin",
        version = "1.0.50" // todo find a way to auto define a static field or something like that
    )
}
