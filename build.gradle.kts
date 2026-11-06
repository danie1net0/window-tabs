import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.changelog") version "2.5.0"
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
        // The plugin runs on the Kotlin stdlib bundled with the IDE; 2024.3, the oldest supported, ships 2.0.
        apiVersion = KotlinVersion.KOTLIN_2_0
        // Kotlin 2.4 warns that API 2.0 is deprecated; that version is the point, so keep the warning out of -Werror.
        freeCompilerArgs.add("-Xsuppress-version-warnings")
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

changelog {
    version = providers.gradleProperty("pluginVersion")
    repositoryUrl = "https://gitlab.com/danie1net0/window-tabs"
}

val pluginVersion = providers.gradleProperty("pluginVersion").get()

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }

        // The Marketplace shows these notes on the version's page; they come from this version's CHANGELOG.md section.
        changeNotes =
            with(changelog) {
                renderItem(getOrNull(pluginVersion) ?: getUnreleased(), Changelog.OutputType.HTML)
            }
    }

    // Only the release pipeline has these variables; locally and on branches the plugin builds unsigned.
    signing {
        certificateChainFile = layout.projectDirectory.file(providers.environmentVariable("CERTIFICATE_CHAIN_FILE"))
        privateKeyFile = layout.projectDirectory.file(providers.environmentVariable("PRIVATE_KEY_FILE"))
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        // The tab strip relies on the internal ideRootPaneNorth extension point, so only real breakage fails the build.
        failureLevel = listOf(FailureLevel.COMPATIBILITY_PROBLEMS, FailureLevel.INVALID_PLUGIN, FailureLevel.MISSING_DEPENDENCIES)
        ides {
            // "IU-2024.3.6,PS-2026.3,...": the oldest supported platform plus the newest of each IDE family.
            providers
                .gradleProperty("verifyIdes")
                .get()
                .split(",")
                .map { it.trim().split("-", limit = 2) }
                .forEach { (code, version) -> create(IntelliJPlatformType.fromCode(code), version) }
        }
    }
}

tasks.verifyPluginSignature {
    dependsOn(tasks.signPlugin)
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
