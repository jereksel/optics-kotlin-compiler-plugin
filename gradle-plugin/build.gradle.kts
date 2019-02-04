plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("org.gradle.maven-publish")
}

gradlePlugin {
    plugins {
        create("debuglog") {
            id = "com.ivianuu.debuglog"
            implementationClass = "com.ivianuu.debuglog.MyGradlePlugin"
        }
    }
}

dependencies {
    implementation(Deps.kotlinStdLib)

    implementation(Deps.kotlinGradlePluginApi)

    compileOnly(Deps.autoService)
    kapt(Deps.autoService)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.ivianuu.debuglog"
            artifactId = project.name
            version = Build.version
            from(components["java"])
        }
    }
}