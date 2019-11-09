package com.ivianuu.debuglog

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class MyClassBuilderIntercepterExtension : ClassBuilderInterceptorExtension {

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = MyClassBuilderFactory(bindingContext, interceptedFactory)

    class MyClassBuilder(
        val bindingContext: BindingContext,
        val declarationOrigin: JvmDeclarationOrigin,
        val delegateBuilder: ClassBuilder
    ) : DelegatingClassBuilder() {

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

        override fun newMethod(
            origin: JvmDeclarationOrigin,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            println("new method $name")

            if (name == "<init>") {
                val initMethod = super.newMethod(origin, access, name, desc, signature, exceptions)

                return object : MethodVisitor(Opcodes.API_VERSION, initMethod) {
                    override fun visitInsn(opcode: Int) {
                        super.visitInsn(opcode)
                        if (opcode == Opcodes.RETURN) {
                            println("return from <init>")
                        }
                    }
                }
            } else {
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }
        }

        override fun newField(
            origin: JvmDeclarationOrigin,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            return if (name.startsWith(DELEGATE_PREFIX)) {
                val delegateField =
                    super.newField(origin, Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC, name, desc, signature, value)

                super.newField(
                    origin,
                    Opcodes.ACC_PUBLIC, name.replace("\$\$", ""), desc, signature, value
                )

                delegateField
            } else {
                super.newField(origin, access, name, desc, signature, value)
            }
        }

        private companion object {
            private const val DELEGATE_PREFIX = "\$\$delegate_"
        }
    }

    private class MyClassBuilderFactory(
        private val bindingContext: BindingContext,
        private val delegateFactory: ClassBuilderFactory
    ) : ClassBuilderFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            return MyClassBuilder(
                bindingContext,
                origin,
                delegateFactory.newClassBuilder(origin)
            )
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

