plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("org.jetbrains.intellij") version Versions.intellijGradlePlugin
}

intellij {
    updateSinceUntilBuild = false
    version = Versions.intellijIdea
    setPlugins("java", Deps.kotlinIdeaPlugin)
}

dependencies {
    implementation(Deps.kotlinStdLib)

    api(project(":kotlin-plugin"))

    compileOnly(Deps.autoService)
    kapt(Deps.autoService)
}
