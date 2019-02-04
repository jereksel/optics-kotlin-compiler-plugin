package com.ivianuu.debuglog

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.name.FqName

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyExpressionCodegenExtension : ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        super.generateClassSyntheticParts(codegen)

        val descriptor = codegen.descriptor

        if (!descriptor.annotations.hasAnnotation(
                FqName("com.ivianuu.myapplication.Synthetics")
            )
        ) {
            return
        }


    }

}