package com.ivianuu.debuglog

data class Parent(
    val a: Int,
    val b: String,
    val c: List<String>
) {

    object MyFun1 : Function1<Parent, Int> {
        override fun invoke(p1: Parent): Int {
            return p1.a
        }
    }

    object MyFun2 : Function2<Parent, String, Parent> {
        override fun invoke(p1: Parent, p2: String): Parent {
            return p1.copy(b = p2)
        }
    }

    object MyFun3 : Function2<Parent, Int, Parent> {
        override fun invoke(p1: Parent, p2: Int): Parent {
            return p1.copy(a = p2)
        }
    }

}