object Build {
    const val version = "1.0.50"
}

object Versions {
    const val autoService = "1.0-rc4"

    const val intellijIdea = "183.5429.30"
    const val intellijGradlePlugin = "0.4.2"

    const val kotlin = "1.3.20"
    const val kotlinIdeaPlugin = "1.3.20-release-IJ2018.3-1"

    const val mavenGradlePlugin = "2.1"

    const val shadow = "4.0.3"
}

object Deps {
    const val autoService = "com.google.auto.service:auto-service:${Versions.autoService}"

    const val intellijGradlePlugin = "org.jetbrains.intellij"

    const val kotlinIdeaPlugin = "org.jetbrains.kotlin:${Versions.kotlinIdeaPlugin}"

    const val kotlinCompilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinGradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    const val mavenGradlePlugin =
        "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradlePlugin}"

    const val shadow = "com.github.jengelman.gradle.plugins:shadow:${Versions.shadow}"
}