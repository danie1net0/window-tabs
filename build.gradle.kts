import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
}

group = "br.dev.ddr.windowtabs"
version = providers.gradleProperty("pluginVersion").get()

// Bytecode for Java 21, built by whatever JDK runs Gradle, so the IDE's bundled JBR is enough on a machine without a JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        allWarningsAsErrors = true
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.24.0")
    mockitoAgent("org.mockito:mockito-core:5.24.0") { isTransitive = false }

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        // The tab strip relies on the internal ideRootPaneNorth extension point, so only real breakage fails the build.
        failureLevel = listOf(FailureLevel.COMPATIBILITY_PROBLEMS, FailureLevel.INVALID_PLUGIN, FailureLevel.MISSING_DEPENDENCIES)
        ides {
            val version = providers.gradleProperty("verifyIdeVersion")
            create(IntelliJPlatformType.PhpStorm, version)
            create(IntelliJPlatformType.GoLand, version)
        }
    }
}

tasks.test {
    // Mockito mocks Swing frames through its inline agent; loading it up front avoids the dynamic-attach warning.
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}

ktlint {
    version = "1.8.0"
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("config/detekt.yml"))
}

kover {
    reports {
        verify {
            rule("All plugin code is covered") {
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                    minValue = 100
                }
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                    minValue = 100
                }
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.koverVerify)
}
