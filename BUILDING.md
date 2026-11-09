# Building Command App

Kotlin + Jetpack Compose, one app module, two flavors:

- **standard** — `net.sentinelsafeguard.commandapp` (the release line)
- **hunt** — `net.sentinelsafeguard.commandapp.hunt` (pre-release builds that
  can sit beside the release line on the same phone)

## Commands

- Unit tests: `./gradlew :app:testStandardDebugUnitTest :app:testHuntDebugUnitTest`
- Compile proof (unsigned release): `./gradlew assembleRelease`
- Signed hunt APK (needs the signing environment below): `./gradlew signedHuntRelease`

## Signing

Release signing follows the fleet pattern: the owner's custodied
keystore reaches the build only through the environment —
`SENTINEL_APP_KEYSTORE` (path to the PKCS12 file),
`SENTINEL_APP_KEYSTORE_PASSWORD`, `SENTINEL_APP_KEY_ALIAS`,
`SENTINEL_APP_KEY_PASSWORD`. All four set signs; some set fails the
build; none set leaves release builds unsigned (compile proofs only).
The signed-hunt GitHub Actions workflow materialises the keystore
from the `release` environment's Actions secrets, verifies the APK with apksigner, rejects the debug
certificate, and asserts the signer SHA-256 equals the fingerprint in
the README.

## Dependency hash lock (fleet law)

**Status (2026-10-30): not yet committed.** The generation workflow
exists and the procedure below is the path; until its output is
committed, builds resolve without hash verification — a known gap
against fleet law, waiting on the same Actions minutes/billing wall
as the rest of this repo's CI. Once the file below is committed,
every artifact Gradle resolves must match its recorded SHA-256 or
the build fails. To change a pinned version:

1. Edit the version.
2. Run the **Generate verification metadata** workflow manually.
3. Download its `verification-metadata` artifact and commit it as
   `gradle/verification-metadata.xml` in the same change.
4. The next normal CI run enforces the new lock.

Dependabot version updates for Gradle are OFF fleet-wide for exactly
this reason (see `.github/dependabot.yml`); Actions updates stay on.

## Backend contract

The app talks to the Command app API from the project spec (§3.3):
base `https://command.sentinelsafeguard.net`, resources under
`/api/app/*`, device token in an `Authorization: Bearer …` header.
Cloudflare Access service-auth headers are added when the values
are supplied at build time (env `SENTINEL_APP_CF_ACCESS_CLIENT_ID` /
`SENTINEL_APP_CF_ACCESS_CLIENT_SECRET` or `-PcfAccessClientId` /
`-PcfAccessClientSecret`). They are **not** plaintext build config:
the `generateGateConfig` Gradle task seals them (AES-GCM) into a
generated source file and the app unseals them at runtime — see
HARDENING.md §2. They ship empty until the Access layer lands, and
the app compiles and tests fine without them.

## Release hardening

Release builds (both flavors) are R8-minified and
resource-shrunk, with keep rules in `app/proguard-rules.pro`.
Each minified build's `mapping.txt` is archived by CI as a build
artifact and is **never committed** — it is the deobfuscation key
for release crash reports. Mail TLS is SPKI-pinned to the mail
server's CA anchors; the command host is deliberately unpinned.
The full story — doctrine, provenance, rotation procedures,
failure modes — is in [HARDENING.md](HARDENING.md).
