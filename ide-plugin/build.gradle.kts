plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij") version Versions.intellijGradlePlugin
}

intellij {
    updateSinceUntilBuild = false
    version = Versions.intellijIdea
    setPlugins("java", Deps.kotlinIdeaPlugin)
}

dependencies {
    implementation(Deps.kotlinStdLib)

    api(project(":kotlin-plugin", configuration = "shadow"))

    testCompile(Deps.arrowOptics)
    testCompile(Deps.kotlinTest)
}

//tasks.withType<Test> {
//    useJUnitPlatform()
//}
