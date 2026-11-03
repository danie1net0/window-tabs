plugins {
    // Downloads a JDK when a task asks for one the machine lacks (detekt wants JDK 21; CI and the Lenovo only have 25).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "window-tabs"
