package com.ivianuu.debuglog

import org.jetbrains.kotlin.name.FqName

object OpticsConst {

    const val LENS_CLASS_NAME = "arrow.optics.PLens"
    private const val ANNOTATION_CLASS_NAME = "arrow.optics.optics"
    const val OPTICS_CLASS_NAME = "Optics"

    val lensClass = FqName(LENS_CLASS_NAME)
    val annotationClass = FqName(ANNOTATION_CLASS_NAME)

}