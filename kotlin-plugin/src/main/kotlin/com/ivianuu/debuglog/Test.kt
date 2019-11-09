package com.ivianuu.debuglog

import org.jetbrains.kotlin.psi.KtFile

class Test(
        val a: String = "a"
) {

    fun getFunction(): Function0<String> {
        return object: Function0<String> {
            override fun invoke(): String {
                println(KtFile::class.java)
               return a
            }
        }
    }

}