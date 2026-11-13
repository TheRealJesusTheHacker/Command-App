import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.3"
}

// ---------------------------------------------------------------------------
// Command App Desktop 1.0.0 — Compose Multiplatform port (desktop
// port #3, 2026-11-13), following the Academy/Mail proven pattern:
// a standalone build compiling SHARED sources directly from
// ../app/src/main/java. For Command the shared layer is the whole
// transport + models + logic layer (config, data/api, every pure
// *Logic file, MirrorNav, CtosBuilders) — those files are pure JVM
// and compile verbatim. The Android UI screens are NOT shared:
// they are coupled to androidx.navigation.NavController (34
// files), LocalContext/activity launchers, osmdroid Android
// views, MediaPlayer, WorkManager and BiometricPrompt; the
// desktop UI in src/main/kotlin is a native Compose Desktop UI
// over the same shared API/logic layer, with per-surface
// dispositions recorded in the program record. The Android
// module is a separate build, untouched by this file.
// ---------------------------------------------------------------------------

val sharedSources = listOf(
    "config/AppConfig.kt",
    "config/CertificatePins.kt",
    "config/GateConfig.kt",
    "config/GateCrypto.kt",
    "data/api/CtosApi.kt",
    "data/api/DoAllApi.kt",
    "data/api/Models.kt",
    "data/api/Phase3Api.kt",
    "data/api/Phase4Api.kt",
    "data/api/Phase5Api.kt",
    "data/api/Phase6Api.kt",
    "data/api/Phase7Api.kt",
    "data/api/Phase8Api.kt",
    "data/api/Phase8bApi.kt",
    "data/api/Phase9Api.kt",
    "data/api/SentinelApi.kt",
    "data/api/Transport.kt",
    "notify/PermissionState.kt",
    "notify/PushPrefsLogic.kt",
    "notify/QuietHours.kt",
    "ui/AnalyticsLogic.kt",
    "ui/CtosBuilders.kt",
    "ui/CtosLogic.kt",
    "ui/FieldValidation.kt",
    "ui/IntelMapLogic.kt",
    "ui/InteractionLogic.kt",
    "ui/MirrorLogic.kt",
    "ui/MirrorNav.kt",
    "ui/OsintLogic.kt",
    "ui/Phase3Logic.kt",
    "ui/Phase4Logic.kt",
    "ui/Phase5Logic.kt",
    "ui/Phase6Logic.kt",
    "ui/Phase7Logic.kt",
    "ui/Phase8Logic.kt",
    "ui/Phase8bLogic.kt",
    "ui/Phase9Logic.kt",
    "ui/PortalBriefs155Logic.kt",
    "ui/TierLogic.kt",
    "ui/Wave161Logic.kt",
    "util/SplashSeason.kt",
    "util/VersionCompare.kt",
)

sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
            srcDir("../app/src/main/java/net/sentinelsafeguard/app")
            include(sharedSources + "net/sentinelsafeguard/app/**/*.kt")
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    // The phone's own transport stack — desktop-JVM native:
    implementation("com.squareup.okhttp3:okhttp:4.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.json:json:20240303")
    // JNA: DPAPI (Windows) and libsecret / Secret Service (Linux)
    // for the credential store chain.
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    // WYS Listen: pure-Java MP3 playback (desktop-only).
    implementation("javazoom:jlayer:1.0.1")
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "net.sentinelsafeguard.app.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi)
            packageName = "command-app"
            packageVersion = "1.0.0"
            description = "Command App — desktop client for Sentinel Command, from Sentinel Safeguard"
            vendor = "Sentinel Safeguard LLC"
            linux {
                packageName = "command-app"
                appCategory = "Office"
                iconFile.set(project.file("icon-192.png"))
            }
            windows {
                packageName = "Command App"
                iconFile.set(project.file("icon-192.png"))
                dirChooser = true
                menuGroup = "Sentinel Safeguard"
                upgradeUuid = "d5e6f7a8-1b2c-4d3e-8f5a-6b7c8d9e0f1a"
            }
        }
    }
}
