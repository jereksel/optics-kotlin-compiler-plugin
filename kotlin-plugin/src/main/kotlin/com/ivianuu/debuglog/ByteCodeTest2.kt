package com.ivianuu.debuglog

import org.jetbrains.kotlin.codegen.AsmUtil


class Fn: Function1<String, Int> {
  override fun invoke(p1: String): Int {
    return p1.length
  }

}

