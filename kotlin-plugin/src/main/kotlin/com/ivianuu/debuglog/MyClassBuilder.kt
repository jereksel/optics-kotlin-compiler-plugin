package com.ivianuu.debuglog

import com.intellij.psi.PsiElement
import jdk.internal.org.objectweb.asm.Opcodes.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.FieldVisitor

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
        println("new field $origin origin kind ${origin.originKind} origin descriptor" +
                " ${origin.descriptor} access $access name $name desc $desc signature $signature value $value")

        return if (name.startsWith(DELEGATE_PREFIX)) {
            println("is delegate")
            super.newField(origin, ACC_PRIVATE or ACC_SYNTHETIC, name, desc, signature, value)
        } else {
            println("is non delegate")
            super.newField(origin, access, name, desc, signature, value)
        }
    }

    private companion object {
        private const val DELEGATE_PREFIX = "\$\$delegate_"
    }
}