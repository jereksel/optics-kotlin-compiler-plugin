package com.jereksel.plugin.optics

import com.ivianuu.debuglog.MyComponentRegistrar
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.joor.Reflect.*

class CompilerTest : StringSpec() {

  init {

    "First test" {

      val kotlinSource = SourceFile.kotlin("Main.kt", """
        import arrow.optics.PLens
        import arrow.optics.optics
        import KClass.Optics.a
        
        @optics
        data class KClass(
          val a: String
        )
        
        fun test(): String {
          val d = KClass("abc")
          return (PLens.id<KClass>().a.modify(d) { "test" }).a
        }
        
    """)

      val result = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        // pass your own instance of a compiler plugin
        compilerPlugins = listOf(MyComponentRegistrar())

        inheritClassPath = true
        messageOutputStream = System.out // see diagnostics in real time
      }.compile()

      result.exitCode shouldBe OK

      on("MainKt", result.classLoader)
          .call("test")
          .get<String>() shouldBe "test"

    }


  }

}