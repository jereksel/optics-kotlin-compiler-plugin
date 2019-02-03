package com.ivianuu.debuglog

import com.intellij.psi.PsiElement
import jdk.internal.org.objectweb.asm.Opcodes
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.FieldVisitor

class MyClassBuilderIntercepterExtension : ClassBuilderInterceptorExtension {

    var messageCollector: MessageCollector? = null

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = MyClassBuilderFactory(bindingContext, interceptedFactory, messageCollector)

    class MyClassBuilder(
        val bindingContext: BindingContext,
        val declarationOrigin: JvmDeclarationOrigin,
        val delegateBuilder: ClassBuilder
    ) : DelegatingClassBuilder() {

        var messageCollector: MessageCollector? = null

        override fun getDelegate() = delegateBuilder

        private var currentClass: KtClassOrObject? = null
        private var currentClassName: String? = null

        override fun defineClass(
            origin: PsiElement?,
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String,
            interfaces: Array<out String>
        ) {
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
            currentClassName = name
        }

        override fun newField(
            origin: JvmDeclarationOrigin,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            println(
                "new field $origin origin kind ${origin.originKind} origin descriptor" +
                        " ${origin.descriptor} access $access name $name desc $desc signature $signature value $value"
            )

            return if (name.startsWith(DELEGATE_PREFIX)) {
                println("is delegate")
                val delegateField =
                    super.newField(origin, Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC, name, desc, signature, value)

                super.newField(
                    origin,
                    Opcodes.ACC_PUBLIC, name.replace("\$\$", ""), desc, signature, value
                )

                delegateField
            } else {
                println("is non delegate")
                super.newField(origin, access, name, desc, signature, value)
            }
        }

        private companion object {
            private const val DELEGATE_PREFIX = "\$\$delegate_"
        }
    }

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

