plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    // extra target, not present in producer
    js {
        browser()
    }

    sourceSets.commonMain {
        dependencies {
            implementation(project(":producer"))
        }
    }
}
