package com.ivianuu.debuglog

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension

@AutoService(ComponentRegistrar::class)
class MyComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }

        // todo can't resolve cliconfigurationskey
        /* val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.NONE)*/

        PackageFragmentProviderExtension.registerExtension(project,
                  MyPackageFragmentProviderExtension().apply {
                      this.messageCollector = messageCollector
                  }
        )

        ExpressionCodegenExtension.registerExtension(project,
            MyExpressionCodegenExtension().apply {
                this.messageCollector = messageCollector
            }
        )

        SyntheticResolveExtension.registerExtension(project, MySyntheticResolveExtension().apply {
            this.messageCollector = messageCollector
        })

        ClassBuilderInterceptorExtension.registerExtension(
            project,
            MyClassBuilderIntercepterExtension().apply {
                this.messageCollector = messageCollector
                //debugLogAnnotations = configuration[KEY_ANNOTATIONS] ?: emptyList()
            }
        )

        StorageComponentContainerContributor.registerExtension(
            project,
            MyStorageComponentContainerContributor()
        )
    }
}

