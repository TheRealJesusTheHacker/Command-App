# Command App — Desktop edition

The desktop port of the Command App Android app (desktop port #3
of the Sentinel desktop-apps program, 2026-11-13), following the
Academy/Mail proven pattern: a standalone Gradle build in
`desktop/` with its own verification metadata, so the Android
build's pins stay byte-identical.

What is shared, verbatim, from `../app/src/main/java`:

- the whole transport + model layer (`config/`, `data/api/` —
  OkHttp 4.12.1 + kotlinx-serialization-json 1.7.3, the
  `/api/app/*` twin contracts, device-token + TOTP sign-in),
- every pure logic file (`ui/*Logic`, `MirrorNav`, `CtosBuilders`,
  notify logic, utils) — including IntelMapLogic's payload
  parsers, Wave161Logic's WYS-audio parser, TierLogic and
  AnalyticsLogic.

The Android UI screens are not shared source: they are coupled
to androidx.navigation.NavController, LocalContext/activity
launchers, osmdroid Android views, MediaPlayer, WorkManager and
BiometricPrompt. The desktop UI (`src/main/kotlin/.../desktop/`
+ `ui/TileMap.kt`) is a native Compose Desktop UI over the same
shared layer, with its own small back-stack (navigation decision
of record). Core surfaces: sign-in (+TOTP), Today (+WYS Listen),
Dashboard, Clients (+owner tier grant), Tickets, Money desk
(invoices), Booking, OSINT (+key-status booleans only), CToS
tools + cases, Live Intel (tile maps), Analytics, Settings,
About.

Honest desktop dispositions (no dead controls):

- Maps: osmdroid cannot run on desktop. Live Intel uses a
  Compose tile map — real OpenStreetMap tiles over HTTPS with an
  identifying user-agent, disk-cached in the config dir, markers
  plotted from the same shared parsers, pan/zoom. The phone's
  satellite/dark tile layers, pulsing-marker animation and
  heading-vector polylines are ABSENT on desktop (recorded, not
  faked).
- WYS Listen: real MP3 playback via JLayer (pure Java); the
  Listen control plays or renders the server's refusal.
- Biometrics: absent — desktop unlock is an optional PIN
  (salted SHA-256 verifier in the desktop store) only.
- Push: the self-hosted poll runs while the app is open
  (about once a minute, with ACK); nothing is delivered while
  the app is closed and nothing claims otherwise.
- Self-updater: the phone's APK updater does NOT exist here —
  no control at all; desktop updates come from the downloads
  page.
- Credentials: DesktopStore chain — DPAPI (Windows/JNA) →
  libsecret Secret Service (Linux/JNA, runtime-probed) →
  AES-256-GCM file fallback (0600 key file, per-user config
  dir `~/.config/CommandApp`); the ACTIVE posture is labeled
  in About.
- Cloudflare Access gate values were never issued for desktop:
  the gate config is empty by design (device token is the
  identity), exactly the phone's pre-Access behavior.
- Strings: the phone's UI is hardcoded English (no Spanish UI
  exists on the phone — a documented program-level gap); the
  desktop UI is English matching the phone. Nothing was
  machine-translated.

Build (Linux .deb):

```
./gradlew test packageDeb
```

Dependencies verify against `gradle/verification-metadata.xml`
(exported from this build's own resolved graph). Windows
packaging (`packageMsi`/`packageExe`) is configured but was NOT
built or verified in this port; it is not claimed as shipped.
