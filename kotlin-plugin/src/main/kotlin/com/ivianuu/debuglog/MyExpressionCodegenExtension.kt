package com.ivianuu.debuglog

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

    var messageCollector: MessageCollector? = null


    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        super.generateClassSyntheticParts(codegen)

        println("generate class synthetic parts ${codegen} ${codegen.descriptor}")

        val descriptor = codegen.descriptor

        if (!descriptor.annotations.hasAnnotation(
                FqName("com.ivianuu.myapplication.Synthetics")
            )
        ) {
            return
        }


        codegen.v.newField(
            JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC, "testProperty",
            codegen.typeMapper.mapType(descriptor.builtIns.anyType).internalName, null, null
        )
    }

}