# Command App — hardening notes

What was hardened, how it works, how to operate it, and — just as
important — what each measure does **not** do. Read this before
changing any of it.

## Doctrine first

Nothing inside an APK can be made truly secret. Anything the app
can use, a determined person holding the APK can extract; the
measures below raise that cost from minutes to an afternoon and
keep values out of *casual* view (repo browsers, `strings` dumps,
scrapers). They are speed bumps, not vaults.

The real gates were never in the app:

- **The device token is the identity** — issued per device by the
  backend after a passphrase sign-in, stored hashed server-side,
  revocable per device. It is generated at runtime and stored in
  Keystore-backed encrypted storage; it appears in no file.
- **Cloudflare Access service-auth is the gate key** — it proves an
  enrolled app copy is calling. A leaked gate key authenticates no
  one without a device token.
- **The backend enforces the role matrix** on every call. The app
  holds no admin capability to steal.

Keep that ordering in mind: if a change below ever conflicts with
those gates, the gates win.

## 1. R8 obfuscation (release + hunt builds)

`release` builds (both flavors — the signed hunt APK is one) run
with `isMinifyEnabled` + `isShrinkResources`. Keep rules live in
`app/proguard-rules.pro` and are enumerated there with reasons:
the kotlinx-serialization wire models (the only reflection-based
serializer surface), the WorkManager poll worker (its class name
is persisted by WorkManager across updates), and the Angus Mail
discovery surface (providers, store/transport classes, mailcap
handlers, ServiceLoader implementations — all loaded by class name
from resource files inside the mail jars). Manifest components are
kept by the AAPT rules AGP supplies; they are not duplicated.

Log stripping: `-assumenosideeffects` removes `Log.d`/`Log.v`
calls from release builds. There are no `Log` calls in the app at
all today (source-verified when the rule was added) — the rule
pins that mechanically so a future debug log cannot ship.

**The mapping file.** Every minified build emits
`app/build/outputs/mapping/<variant>/mapping.txt` — the only way
to turn an obfuscated crash stack trace back into source. CI
archives it as a build artifact (`r8-mapping-*`, 90-day
retention). **It is never committed**: a committed mapping hands
the deobfuscation key to anyone who can read the repo. To read a
crash from a shipped build, download that build's mapping
artifact and retrace with the APK's version in hand. If the
artifact has expired, that build's crashes are unreadable —
accept a new build instead of trying to reconstruct it.

## 2. Gate values — sealed config plumbing

Path the Cloudflare Access values travel:

```
GitHub Secrets (or local env / -P properties)
  → build environment: SENTINEL_APP_CF_ACCESS_CLIENT_ID / _SECRET
  → Gradle task generateGateConfig (app/build.gradle.kts)
      seals each value with AES-GCM ("gate-config v1")
      into app/build/generated/source/gateconfig/GeneratedGateConfig.kt
      (build dir, git-ignored, regenerated every build, never committed)
  → runtime: config/GateConfig.kt unseals via config/GateCrypto.kt
  → config/AppConfig.kt hands the pair to the API client
```

Format: key = SHA-256 of three fragment constants joined by `:`;
blob = Base64(12-byte IV ‖ AES-GCM ciphertext+tag). The fragments
live in `GateCrypto.kt` **in the open, on purpose** — the Gradle
generator parses them out of that file, so sealer and unsealer can
never drift apart on material, and `GateCryptoTest` pins the
unsealer against a blob sealed by an independent implementation of
the generator's algorithm. JDK crypto (`javax.crypto`) only; no
dependency was added.

Failure mode is safe by construction: an unseal failure (corrupt
blob, wrong key) yields `""`, the API client omits the Access
headers, and the app reports the server's refusal in plain
language. Nothing crashes, nothing sends a garbage header.

Rotation: the gate values themselves rotate in Cloudflare (Access
service tokens) and in the GitHub Secrets; a new build then seals
the new values. The fragments change only if the *format* is ever
revised — that is a code change (GateCrypto + generator + tests
move together), not an operational task.

## 3. Certificate pinning — mail pinned, command host not

### Mail (`mail.sentinelsafeguard.net`) — pinned to CA anchors

The app pins the two root anchors in the chain the live server
presents. The pins were computed from the chain fetched from
`mail.sentinelsafeguard.net:993` on 2026-10-25:

| Chain position | Certificate | SPKI SHA-256 pin |
|---|---|---|
| leaf | CN=mail.sentinelsafeguard.net (Let's Encrypt) | *not pinned — renews ~90 days* |
| intermediate | Let's Encrypt YE2 | *not pinned — rotates by design* |
| root (cross-signed) | ISRG Root YE | `sCkq5UWXjg+7mKu9lMhhYF5bGLsy7VI/UNW3tccdR7w=` |
| root (cross-signed) | ISRG Root X2 | `diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvVFZE8zmgzI=` |

Pin = Base64(SHA-256(DER SubjectPublicKeyInfo)), computed with:

```
openssl s_client -connect mail.sentinelsafeguard.net:993 \
  -servername mail.sentinelsafeguard.net -showcerts
# per certificate:
openssl x509 -pubkey -noout | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary | base64
```

Why anchors: leaf renewals (~90 days) and intermediate rotations
then change nothing — any future chain running through either root
key still matches, and each pin backs up the other. ISRG Root X1
was considered and rejected on evidence: the X1 certificate is
never *presented* in the chain, so a presented-chain check could
never match its pin.

Where it lives: `config/CertificatePins.kt` (values + checker),
`data/mail/SpkiPinningTrustManager.kt` (platform validation FIRST,
pin check second — a pin can only tighten validation, never loosen
it), `data/mail/MailTls.kt` (the factory), wired into both mail
sessions in `data/mail/MailService.kt` through the
`mail.<protocol>.ssl.socketFactory` session property — the
property Angus consults first for implicit TLS (IMAP 993) and for
the STARTTLS upgrade (SMTP 587); verified against the Angus 2.0.3
`SocketFetcher` source.

**Removed in the same change:** `mail.imap.ssl.trust`. In Angus
that property installs a trust manager that skips chain validation
entirely for the listed host — the exact opposite of pinning. It
is gone; do not reintroduce it.

### Command host (`command.sentinelsafeguard.net`) — NOT pinned

Deliberate exception. The certificate on that hostname is
presented through Cloudflare's layer, which the owner does not
control: Cloudflare can re-present the hostname under a different
CA without any action or change on the owner's server, and the
day it does, a pin there bricks the app **and** its token-gated
update channel at the same moment. That channel's protections are
platform TLS, the Access service-auth gate, and the revocable
device token — in that design a pin adds fragility, not strength.
`CertificatePins.COMMAND_PINS` is empty and a unit test keeps it
empty; changing this means revisiting the design, not the value.

### Rotation procedure (mail pins)

Needed only if the mail server's CA or key type changes (e.g. a
different CA, or RSA↔ECDSA switch) or Let's Encrypt retires the
Generation Y roots — Let's Encrypt *operational* changes (leaf
renewals, new intermediates) need nothing.

1. Fetch the live chain (command above) and compute the anchor
   pins from the presented root certificates.
2. Update `CertificatePins.MAIL_PINS` **and** the golden values in
   `CertificatePinsTest` together — the test failing on a pin edit
   is the intended tripwire.
3. Ship a normal app release. The update channel still works in
   the meantime (it does not depend on the mail pins), so phones
   can pull the fixed build themselves; a crew-delivered APK is
   the fallback, as always.

### Failure mode (what the owner sees)

If the mail pins ever stop matching (an unplanned CA change), the
app's mail tab fails its TLS handshake and shows a connection
error in plain language. Nothing else is affected: webmail works,
the Command half of the app works, updates work. There is no
silent insecure fallback — mail fails closed, by design, and the
fix is the rotation procedure above.

## 4. What remains build-gated

These notes describe code and configuration as committed. The
first minified build, the execution of the full Gradle test task,
and the signed hunt APK all still wait on CI (see BUILD-1.md,
Wall 1). The new crypto/pin code was compile- and logic-verified
by its authors against the real dependency jars; the Gradle build
is the authority when it runs.
