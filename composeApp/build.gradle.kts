import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.sqlite.jdbc)
            implementation(libs.kaml)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }
        desktopTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.bolobhai.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "com.bolobhai.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "Bolo Bhai"
            packageVersion = "1.0.0"
            vendor = "Bolo Bhai"
            description = "Local offline Hinglish voice dictation"

            // Bundle the sidecar binaries (whisper-server, llama-server, mac helper) into the
            // install folder. Compose copies sidecars/<os>/ and exposes it at runtime via the
            // compose.application.resources.dir system property (see platform/Sidecars.kt).
            appResourcesRootDir.set(project.rootProject.layout.projectDirectory.dir("sidecars"))

            windows {
                iconFile.set(project.rootProject.file("resources/icons/bolobhai.ico"))
                menuGroup = "Bolo Bhai"
                perUserInstall = true
                shortcut = true
                // Stable UUID so upgrades replace the previous install.
                upgradeUuid = "4b1d6f8e-9c2a-4e37-9a1e-7c2b3d4e5f60"
            }
            macOS {
                iconFile.set(project.rootProject.file("resources/icons/bolobhai.icns"))
                bundleID = "com.elevationsoft.bolobhai.app"
            }
        }
    }
}
