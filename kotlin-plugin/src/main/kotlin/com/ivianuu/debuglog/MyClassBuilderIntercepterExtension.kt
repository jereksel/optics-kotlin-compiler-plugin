package com.ivianuu.debuglog

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

class MyClassBuilderIntercepterExtension : ClassBuilderInterceptorExtension {

    var messageCollector: MessageCollector? = null

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = MyClassBuilderFactory(bindingContext, interceptedFactory, messageCollector)

    private class MyClassBuilderFactory(
        private val bindingContext: BindingContext,
        private val delegateFactory: ClassBuilderFactory,
        private var messageCollector: MessageCollector?
    ) : ClassBuilderFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            return MyClassBuilder(
                bindingContext,
                origin,
                delegateFactory.newClassBuilder(origin)
            ).apply {
                this.messageCollector = this@MyClassBuilderFactory.messageCollector
            }
        }

        override fun getClassBuilderMode() = delegateFactory.classBuilderMode

        override fun asText(builder: ClassBuilder?): String? {
            return delegateFactory.asText((builder as MyClassBuilder).delegateBuilder)
        }

        override fun asBytes(builder: ClassBuilder?): ByteArray? {
            return delegateFactory.asBytes((builder as MyClassBuilder).delegateBuilder)
        }

        override fun close() {
            delegateFactory.close()
        }
    }
}

