plugins {
    id("org.sonarqube") version Versions.sonatypePlugin
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Deps.kotlinGradlePlugin)
        classpath(Deps.mavenGradlePlugin)
        classpath(Deps.shadow)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}
