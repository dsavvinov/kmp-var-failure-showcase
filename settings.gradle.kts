pluginManagement {
    plugins {
        kotlin("multiplatform") version "1.8.0"
    }

    repositories {
        mavenCentral()
    }
}

rootProject.name = "kmp-var-failure-showcase"

include("consumer", "producer")
