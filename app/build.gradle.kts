plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ---------------------------------------------------------------------------
// Release signing — the owner's custodied key only, never the debug key
// (Sphinx pattern, secret names per the Sentinel App repo's CI secrets).
//
// Signing material comes ONLY from the environment; nothing secret is
// stored in this repo:
//   SENTINEL_APP_KEYSTORE           path to the owner's PKCS12 keystore file
//   SENTINEL_APP_KEYSTORE_PASSWORD  keystore password
//   SENTINEL_APP_KEY_ALIAS          key alias inside the keystore
//   SENTINEL_APP_KEY_PASSWORD       key password
//
// All four set  -> release builds sign with the owner's key.
// Some set      -> configuration fails: a half-set signing setup is a
//                  mistake, not a mode. No fallback key exists.
// None set      -> release builds stay UNSIGNED (compile proof only);
//                  the signed* tasks below refuse to run.
// ---------------------------------------------------------------------------
val signingEnvNames = listOf(
    "SENTINEL_APP_KEYSTORE",
    "SENTINEL_APP_KEYSTORE_PASSWORD",
    "SENTINEL_APP_KEY_ALIAS",
    "SENTINEL_APP_KEY_PASSWORD",
)
val signingEnv = signingEnvNames.associateWith { System.getenv(it) }
val presentSigningEnv = signingEnv.filterValues { !it.isNullOrBlank() }
val releaseSigningReady = presentSigningEnv.size == signingEnvNames.size

if (presentSigningEnv.isNotEmpty() && !releaseSigningReady) {
    throw GradleException(
        "Sentinel App release signing is only partly configured. Set all four of " +
            signingEnvNames.joinToString(", ") + " — or none of them. " +
            "A signed build needs the owner's custodied keystore; there is no fallback key."
    )
}

// Cloudflare Access service-auth values arrive with the Access layer.
// They are build-time values (env or -P properties), never committed.
// Empty placeholders keep the app compiling and its non-network tests
// passing before the Access layer lands; the backend device token is
// the identity either way (spec §3.1) — these headers are a gate key.
val cfAccessClientId: String =
    providers.environmentVariable("SENTINEL_APP_CF_ACCESS_CLIENT_ID").orNull
        ?: providers.gradleProperty("cfAccessClientId").orNull ?: ""
val cfAccessClientSecret: String =
    providers.environmentVariable("SENTINEL_APP_CF_ACCESS_CLIENT_SECRET").orNull
        ?: providers.gradleProperty("cfAccessClientSecret").orNull ?: ""

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "net.sentinelsafeguard.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.sentinelsafeguard.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://command.sentinelsafeguard.net\"")
        buildConfigField("String", "MAIL_HOST", "\"mail.sentinelsafeguard.net\"")
        buildConfigField("String", "CF_ACCESS_CLIENT_ID", cfAccessClientId.asBuildConfigString())
        buildConfigField("String", "CF_ACCESS_CLIENT_SECRET", cfAccessClientSecret.asBuildConfigString())
    }

    flavorDimensions += "line"

    productFlavors {
        create("standard") {
            dimension = "line"
            applicationId = "net.sentinelsafeguard.app"
        }
        create("hunt") {
            dimension = "line"
            applicationId = "net.sentinelsafeguard.app.hunt"
            versionNameSuffix = "-hunt1"
        }
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(signingEnv.getValue("SENTINEL_APP_KEYSTORE")!!)
                storePassword = signingEnv.getValue("SENTINEL_APP_KEYSTORE_PASSWORD")
                keyAlias = signingEnv.getValue("SENTINEL_APP_KEY_ALIAS")
                keyPassword = signingEnv.getValue("SENTINEL_APP_KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/INDEX.LIST",
            )
        }
    }
}

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails unless the owner's release signing is fully configured in the environment."
    doLast {
        if (!releaseSigningReady) {
            throw GradleException(
                "No signed Sentinel App build can be made here: the signing environment is not set. " +
                    "Set SENTINEL_APP_KEYSTORE, SENTINEL_APP_KEYSTORE_PASSWORD, " +
                    "SENTINEL_APP_KEY_ALIAS and SENTINEL_APP_KEY_PASSWORD from the owner's " +
                    "custodied keystore secrets, then run the signed task again. Unsigned and " +
                    "debug-signed builds are compile proofs only — never releases."
            )
        }
    }
}

tasks.register("signedHuntRelease") {
    group = "build"
    description = "Builds the signed HUNT release APK with the owner's keystore (refuses without it)."
    dependsOn(verifyReleaseSigning, "assembleHuntRelease")
}

tasks.register("signedStandardRelease") {
    group = "build"
    description = "Builds the signed standard release APK with the owner's keystore (refuses without it)."
    dependsOn(verifyReleaseSigning, "assembleStandardRelease")
}

tasks.configureEach {
    if (name == "assembleHuntRelease" || name == "assembleStandardRelease") {
        mustRunAfter(verifyReleaseSigning)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Command API client
    implementation("com.squareup.okhttp3:okhttp:4.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Mail: Eclipse Angus Mail (Jakarta Mail implementation, MPL/EPL —
    // maintained by the Eclipse Foundation; spec §4.1 library rule).
    implementation("org.eclipse.angus:angus-mail:2.0.3")
    implementation("org.eclipse.angus:angus-activation:2.0.2")

    // On-device security: Keystore-backed encrypted storage + biometrics
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // Notification polling
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
