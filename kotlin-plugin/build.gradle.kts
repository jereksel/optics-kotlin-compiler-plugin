import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("org.jetbrains.intellij") version Versions.intellijGradlePlugin
    id("com.github.johnrengelman.shadow")
    id("org.gradle.maven-publish")
}

// just a dummy to access intellij and kotlin plugin apis
// todo find a better way
intellij {
    version = Versions.intellijIdea
    setPlugins(Deps.kotlinIdeaPlugin)
}

dependencies {
    implementation(Deps.kotlinStdLib)
    compileOnly(Deps.autoService)
    kapt(Deps.autoService)
}

// the following code makes sure that our kotlin-plugin is compatible with the kotlin-compiler-embeddable package
// which is used by gradle

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
    listOf(
        "com.intellij",
        "com.google",
        "com.sampullara",
        "org.apache",
        "org.jdom",
        "org.picocontainer",
        "org.jline",
        "org.fusesource",
        "kotlinx.coroutines"
    )

val shadowJar: ShadowJar by tasks

shadowJar.apply {
    classifier = ""
    relocate("com.google.protobuf", "$kotlinEmbeddableRootPackage.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    // todo relocate("javax.inject", "$kotlinEmbeddableRootPackage.javax.inject")
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.ivianuu.debuglog"
            artifactId = project.name
            version = Build.version

            //from(components["java"])

            shadow.component(this)
        }
    }
}