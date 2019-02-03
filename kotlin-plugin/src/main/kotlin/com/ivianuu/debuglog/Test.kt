package com.ivianuu.debuglog

interface MyInterface

class MyInterfaceImpl : MyInterface

class Test : MyInterface by MyInterfaceImpl() {

}