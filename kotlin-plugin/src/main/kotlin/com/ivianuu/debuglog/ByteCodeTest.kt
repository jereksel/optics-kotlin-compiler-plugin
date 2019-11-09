package com.ivianuu.debuglog

import com.ivianuu.debuglog.ByteCodeTest.ABC.a

class ByteCodeTest {

  object ABC {

    val String.a get() = ""

    class F: Function<Unit> {

    }

  }

}

fun test() {


  println("as".a)

}