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
// (Sphinx pattern, secret names per the Command App repo's CI secrets).
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
        "Command App release signing is only partly configured. Set all four of " +
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
        applicationId = "net.sentinelsafeguard.commandapp"
        minSdk = 26
        targetSdk = 35
        // 1.4.0 RELEASE (Do All Program, 2026-11-07): the full
        // polish + features + security build over 1.3.0 — wider
        // M-08 morphs, inline validation, pull damping, sliding
        // chip indicator, swipe surfaces, notification-permission
        // explanation, draft preservation, offline sync queue,
        // selfhost push consumption, voice-note playback,
        // backup-bundle download, U-04 updater verification,
        // phone-automation surfaces, native Live Intel maps
        // (OSMDroid), and the alive/premium design elevation
        // (Skyler's mid-run correction: not flat — alive,
        // premium, professional, Sentinel). versionCode 104
        // steps over 1.3.0's 103 so it upgrades in place.
        // Hotfix 1.4.1 (owner report 2026-11-07): the permission
        // state model — a decided-denied POST_NOTIFICATIONS now
        // routes the explainer's button to Android's notification
        // settings instead of a prompt that never appears, and
        // the grant state is re-read on resume. versionCode 105
        // steps over 104 so it upgrades in place.
        // 1.5.0 (2026-11-07): the OSINT deck wires Command's nine
        // new live server legs — subdomain finder, DNS explorer,
        // IP intelligence (map plot when the payload carries
        // coordinates), site inspector, Wayback snapshot browser,
        // BTC/ETH wallet lookups, the two-corpus email breach
        // battery, server-side username variations and the new
        // dork template keys. versionCode 106 steps over 105 so
        // it upgrades in place.
        // Hotfix 1.5.1 (2026-11-08): the push-registration latch.
        // A registration made against the pre-activation server
        // latched registered=true forever, so the device never
        // re-registered once push actually activated. Now the
        // registration records its versionCode and a stale (or
        // unrecorded — every pre-1.5.1 install) registration
        // heals once per version, and sign-out resets the state.
        // versionCode 107 steps over 106 so it upgrades in place.
        // 1.5.2 (2026-11-08): two features from Sentinel Live's
        // Elevation Program relay bundle. F4: the app Settings
        // mirror the server-side push event preferences
        // (GET/POST /api/app/push/prefs — one store with the
        // web actions; the server's values are rendered, never
        // a local copy). F3: OSINT pivot actions in the deck —
        // server-offered chained runs from a completed result,
        // purpose carried by the server, lineage shown.
        // versionCode 108 steps over 107 so it upgrades in place.
        // 1.5.3 (2026-11-08, owner order — LOGO RULE): the launcher
        // icon is now Skyler's real Sentinel Safeguard logo (the
        // icon-sentinel-logo package, generated from
        // your_files/brand/sentinel-safeguard-logo.jpg), replacing
        // the drawn shield-and-checkmark vector foreground, which
        // is retired and pinned absent by LauncherIconTest. The
        // notification small icon likewise becomes a silhouette
        // mechanically derived from the real logo's triangle
        // emblem. versionCode 109 steps over 108 so it upgrades
        // in place.
        // 1.5.4 (2026-11-09): three pieces from Sentinel Live's
        // role-structure window. (A) Native website analytics:
        // a Dashboard card + full view over GET
        // /api/app/analytics — the web analyticssummary payload
        // byte-for-byte, admin:read-gated server-side; the app
        // renders only server values and hides the card whole
        // on the gate's 403. (B) structureRole ("employee" /
        // "client", additive on pub_user/pub_self) is parsed
        // and shown as a quiet chip in Settings; routing still
        // follows the server's role. (C) The retired-role
        // cleanup: production can no longer mint "Family" or
        // "Tourist" accounts, so the portal-by-Family branch,
        // the refused-by-Tourist shell, the directory's "Demo
        // access" section/filter and the role picker's retired
        // options are removed; unrecognized role strings land
        // in ONE defensive default (the portal shell — least
        // privilege), logged, never the Command shell.
        // versionCode 110 steps over 109 so it upgrades in place.
        // 1.5.5 (2026-11-09): client-portal parity for the
        // Combined window's two delivered sections + honest
        // auto-delivered brief labeling. (A) The getmyportal
        // payload's threatBriefs ({id, period, title, body,
        // deliveredAt}) and monthlyReports ({id, period, body,
        // deliveredAt}) sections render natively in the portal's
        // Your Documents view — read-only bodies, no download
        // control (the payload carries no document field), the
        // honest empty lines when nothing is delivered, absent
        // keys degrading to the same empty state as web's
        // `|| []` reads. (B) Staff brief rows: state "sent"
        // with sentBy carried as an explicit null (the engine
        // now delivers briefs to the portal at generation) is
        // labeled "Delivered automatically" — never blank,
        // never a fabricated sender; a real sentBy, a draft,
        // or a payload with no sentBy key gets no note.
        // versionCode 111 steps over 110 so it upgrades in place.
        // 1.6.0 (2026-11-09, owner order — THE RENAME): the app
        // is renamed completely, repo and all: Sentinel App ->
        // Command App. GitHub repo TheRealJesusTheHacker/Sentinel-App
        // -> TheRealJesusTheHacker/Command-App; applicationId
        // net.sentinelsafeguard.app -> net.sentinelsafeguard.commandapp
        // (same custody signer — this is a new package identity,
        // NOT an in-place upgrade: it installs fresh beside the old
        // app, and old-package data does not migrate); launcher
        // label + all app chrome now read "Command App"; the
        // hosted file pattern is Command-App-<version>.apk. The
        // Kotlin namespace/source package stays
        // net.sentinelsafeguard.app — internal organization only;
        // Android identity is the applicationId. Full mapping in
        // the PROGRAM-RECORD rename addendum. versionCode 112.
        // 1.6.1 (2026-11-09, Feature Wave mirror): the app
        // consumes the wave's new payload fields — the owner's
        // While You Slept dashboard card + briefing audio
        // (Items 7/22), the staff client-health score block
        // (Item 14), the stage-ordered photo timeline on ticket
        // profile + portal progress (Item 23), booking prep
        // lists (Item 26), and the two new Pipeline lead
        // sources (Item 13). Strictly additive: every surface
        // renders only when its payload carries the new field.
        // Full detail in the PROGRAM-RECORD 1.6.1 addendum.
        // versionCode 113 steps over 112 so it upgrades in place.
        // 1.6.2 (2026-11-09): the briefing-audio read moves to
        // the canonical app route (GET /api/app/wysaudio) now
        // that the server ships the wrap; the /actions device
        // arm 1.6.1 rode stays live server-side, uncalled by
        // the app. The audio contract fixtures flip to the
        // live shapes (owner available / unavailable, the
        // wrap's 403 and 401 bodies). Everything else is
        // 1.6.1's surface, unchanged. Full detail in the
        // PROGRAM-RECORD 1.6.2 addendum. versionCode 114 steps
        // over 113 so it upgrades in place.
        // 1.6.3 (2026-11-10, owner order): the Mail tab and the
        // entire direct mail engine are REMOVED from the
        // Command App — the app no longer interacts with the
        // mail side of the server at all. The Sentinel Mail App
        // and browser webmail are the only mail clients now; the
        // mail server itself is untouched. Gone: the mail
        // screens, the data/mail engine package (with its SPKI
        // pinning), the mail library dependencies, the
        // mailbox credential store, the Settings mail section,
        // the PollWorker mailbox unread check and the "New mail"
        // notification channel / category / toggle. Command-API
        // surfaces that merely
        // carry email-shaped data (client contact addresses, the
        // client-depth mail bridge, invoice recipientEmail) are
        // Command mirror data, not the mail engine, and stay.
        // Full detail in the PROGRAM-RECORD 1.6.3 addendum.
        // versionCode 115 steps over 114 so it upgrades in place.
        // 1.6.4 (2026-11-10, owner ruling): the client-depth
        // mail bridge KEPT by 1.6.3 now goes too — the "Mail
        // with this client" card, the clientMail /
        // clientMailThread API functions and their contract
        // test are removed. The owner's direction is that mail
        // lives in the Sentinel Mail App and browser webmail
        // only, and the bridge's live data feed was never
        // provisioned, so the card had no live purpose. The
        // server endpoint itself is untouched (this is an
        // app-side removal), and the mirror can regain a
        // correspondence surface later only as its own scoped
        // decision. Full detail in the PROGRAM-RECORD 1.6.4
        // addendum. versionCode 116 steps over 115 so it
        // upgrades in place.
        // 1.6.5 (2026-11-11, owner ruling): ONE vocabulary
        // change — "Active Partner" joins MirrorVocab's
        // CLIENT_STAGES immediately after "Active Client".
        // A partner is not a client: clients receive services
        // from Sentinel; partners trade services mutually.
        // The app learns the stage FIRST (owner's sequencing)
        // so the server-side stage, when it lands, renders on
        // the pipeline board with no gap. Nothing else moves:
        // no features, no other vocabulary, no mail (the
        // separation stands), no auth changes. versionCode
        // 117 steps over 116 so it upgrades in place.
        // 1.7.0 (2026-11-11, CTOS-APP): the native "OSINT
        // tools" section — CToS Wave 1 in the OSINT view, on
        // the /api/app/osint/ctos/* twins of the web ctos*
        // actions (same handler cores, same osint gate): the
        // case bar (create / open / export / clear / delete),
        // case files with type-filtered findings and manual
        // records, the full Wave 1 tool deck with native
        // forms and structured results (profiler statuses stay
        // honest — unknown is never a negative), the File Hash
        // Generator computed on-device exactly as web computes
        // it in the browser, and a premium-sources card showing
        // configured state only (keys are entered in web
        // Command settings; the app never accepts one).
        // versionCode 118 steps over 117 so it upgrades in
        // place over 1.6.5.
        // 1.7.1 (2026-11-11, AUDIT-171): the full bug-hunt +
        // security-audit + polish fix pass over 1.7.0. Security:
        // the self-updater fails CLOSED (a version payload with
        // no SHA-256 is a refusal, and the downloaded APK's
        // signer must match the installed app's before install);
        // FLAG_SECURE; the client save can no longer carry a
        // tier field (T-1). Push: deep links land on their
        // target, batches no longer collapse, seen-id memory is
        // recorded before the ACK, sign-out unregisters.
        // OSINT [OSINT]: the five CToS command builders and the
        // OPSEC profile panel are real (they never reach the
        // server run path), the printable case report has its
        // screen, gated tools are badged, server URLs pass an
        // http/https allowlist. Offline: the ticket form queues
        // on transport failure and flushes via /api/app/sync.
        // Polish: every Material 3 colour role is set (no
        // baseline purple anywhere), error text meets 4.5:1,
        // Today loads in parallel with honest retry states.
        // Suites 554 → 593 green; staging rehearsal 18/18.
        // versionCode 119 steps over 118 so it upgrades in
        // place over 1.7.0.
        //
        // 1.7.2 (TIER Phase B wiring): the client record's
        // server-computed tierState is parsed everywhere and is
        // the source of truth for tier display + charge gating
        // (the record's `tier` key stays the triage tag, never
        // billing). The profile Tier card gains the owner-only
        // grant control at the AUDIT-171 seam: three-state
        // Family / Friends / Standard, rendered only for the
        // OWNER role, writing via POST /api/app/clients/{id}/tier
        // with the server's refusal messages verbatim and the
        // fresh record applied on success. S-2: the OPSEC
        // profile panel saves via POST /api/app/osint/ctos/
        // profile, sending only changed fields (absent =
        // preserve). versionCode 120 steps over 119 so it
        // upgrades in place over 1.7.1.
        //
        // 1.7.3 (W-5 key-status): the OSINT deck's "Source API
        // keys" card becomes the full key-status surface. Key
        // state is a tri-state (Configured / Not configured /
        // Status unavailable) read from the workspace payload's
        // keySources strict booleans — a value that is not a
        // strict boolean, and a payload with no keySources at
        // all, render "Status unavailable", never a guessed
        // state. Not-configured sources carry one honest line
        // on what a key unlocks and that keys are set in web
        // Command settings; the app still never asks for,
        // accepts, or stores a key (values are web-only by
        // design). versionCode 121 steps over 120 so it
        // upgrades in place over 1.7.2.
        versionCode = 121
        versionName = "1.7.3"

        buildConfigField("String", "API_BASE_URL", "\"https://command.sentinelsafeguard.net\"")
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
            applicationId = "net.sentinelsafeguard.commandapp"
        }
        create("hunt") {
            dimension = "line"
            applicationId = "net.sentinelsafeguard.commandapp.hunt"
            // hunt18 TEST build (GUI elevation): installs as an
            // update over every earlier hunt build — the pre-release
            // hunt series ran versionCode 1..17 and the 1.0.0 head's
            // hunt flavor would carry the shared 100 — so the hunt
            // flavor steps to 101 here. The standard flavor keeps
            // the release fields in defaultConfig untouched.
            versionCode = 101
            versionNameSuffix = "-hunt18"
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
                "No signed Command App build can be made here: the signing environment is not set. " +
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

    // On-device security: Keystore-backed encrypted storage + biometrics
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // Notification polling
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Live Intel maps (1.4.0): OSMDroid — native map view, keyless
    // tile layers (OSM streets / Esri satellite / CARTO dark),
    // Apache-2.0. Hash-locked in gradle/verification-metadata.xml
    // like every other dependency.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
