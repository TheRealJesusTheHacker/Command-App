import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

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
// They are build-time values (env or -P properties), never committed,
// and they are NEVER written into the app in plaintext: the
// generateGateConfig task below seals them (AES-GCM) into a generated
// source file, and the app unseals them at runtime via GateCrypto.
// Empty values keep the app compiling and its non-network tests
// passing before the Access layer lands; the backend device token is
// the identity either way (spec §3.1) — these headers are a gate key.
val cfAccessClientId: String =
    providers.environmentVariable("SENTINEL_APP_CF_ACCESS_CLIENT_ID").orNull
        ?: providers.gradleProperty("cfAccessClientId").orNull ?: ""
val cfAccessClientSecret: String =
    providers.environmentVariable("SENTINEL_APP_CF_ACCESS_CLIENT_SECRET").orNull
        ?: providers.gradleProperty("cfAccessClientSecret").orNull ?: ""

// Where the sealed gate config source is generated (build dir —
// ephemeral, never committed; /app/build/ is git-ignored).
val gateConfigOutDir = layout.buildDirectory.dir("generated/source/gateconfig")

android {
    namespace = "net.sentinelsafeguard.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.sentinelsafeguard.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://command.sentinelsafeguard.net\"")
        buildConfigField("String", "MAIL_HOST", "\"mail.sentinelsafeguard.net\"")
        // The CF Access values are deliberately NOT build config
        // fields — plaintext BuildConfig strings are readable from
        // the APK with `strings`. They travel sealed instead; see
        // the generateGateConfig task and config/GateCrypto.kt.
    }

    sourceSets {
        getByName("main") {
            java.srcDir(gateConfigOutDir)
        }
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
            versionNameSuffix = "-hunt4"
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
            // Obfuscation + shrinking for every release build (both
            // flavors — the signed hunt APK is a release build). The
            // keep rules in proguard-rules.pro are deliberate and
            // enumerated; the mapping file R8 emits is archived by
            // CI as a build artifact and is NEVER committed (it is
            // the deobfuscation key for release crash reports).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

// ---------------------------------------------------------------------------
// Gate config generator — seals the CF Access values into source.
//
// Runs before every Kotlin compile. Reads the key fragments out of
// config/GateCrypto.kt (the single source of truth, so the sealer here
// and the unsealer at runtime cannot drift apart on material), seals
// the two build-time values with AES-GCM (the "gate-config v1" format
// GateCrypto documents), round-trip-checks its own output, and writes
// GeneratedGateConfig.kt into the build directory. With no values
// configured it seals empty strings, and the app then behaves as
// "Access not configured" — the state it ships in until the Access
// layer lands. JDK crypto only, the same java.* APIs the runtime
// side uses; no new dependency on either side.
// ---------------------------------------------------------------------------
val generateGateConfig by tasks.registering {
    group = "build"
    description = "Seals the Cloudflare Access gate values into GeneratedGateConfig.kt (AES-GCM blobs, never plaintext)."
    val outDir = gateConfigOutDir.get().asFile
    outputs.dir(outDir)
    doLast {
        val gateCryptoSource =
            file("src/main/java/net/sentinelsafeguard/app/config/GateCrypto.kt")
        val fragments = Regex("""FRAGMENT_\d+ = "([A-Za-z0-9+/=]+)"""")
            .findAll(gateCryptoSource.readText())
            .map { it.groupValues[1] }
            .toList()
        if (fragments.size != 3) {
            throw GradleException(
                "Gate config generation failed: expected 3 FRAGMENT_n constants " +
                    "in GateCrypto.kt, found ${fragments.size}. The fragment lines " +
                    "are part of the gate-config v1 contract — see GateCrypto.kt."
            )
        }
        val key = javax.crypto.spec.SecretKeySpec(
            MessageDigest.getInstance("SHA-256")
                .digest(fragments.joinToString(":").toByteArray(Charsets.UTF_8)),
            "AES",
        )
        fun seal(plain: String): String {
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                javax.crypto.Cipher.ENCRYPT_MODE, key,
                javax.crypto.spec.GCMParameterSpec(128, iv),
            )
            return Base64.getEncoder()
                .encodeToString(iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
        }
        fun unseal(blob: String): String {
            val raw = Base64.getDecoder().decode(blob)
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                javax.crypto.Cipher.DECRYPT_MODE, key,
                javax.crypto.spec.GCMParameterSpec(128, raw.copyOfRange(0, 12)),
            )
            return String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
        }
        val idBlob = seal(cfAccessClientId)
        val secretBlob = seal(cfAccessClientSecret)
        if (unseal(idBlob) != cfAccessClientId || unseal(secretBlob) != cfAccessClientSecret) {
            throw GradleException("Gate config generation failed its own round-trip check.")
        }
        outDir.mkdirs()
        File(outDir, "GeneratedGateConfig.kt").writeText(
            """
            package net.sentinelsafeguard.app.config

            /**
             * GENERATED by the generateGateConfig Gradle task — do not edit,
             * do not commit. The values are AES-GCM-sealed blobs ("gate-config
             * v1"); GateCrypto unseals them at runtime. See HARDENING.md.
             */
            internal object GeneratedGateConfig {
                const val CF_ACCESS_CLIENT_ID_BLOB: String = "$idBlob"
                const val CF_ACCESS_CLIENT_SECRET_BLOB: String = "$secretBlob"
            }
            """.trimIndent() + "\n"
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateGateConfig)
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
