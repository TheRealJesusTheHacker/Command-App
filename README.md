# Sentinel App

Private Android app for **Sentinel Command** and **Sentinel Mail** — the owner's phone
front-end for Sentinel Safeguard's private command system and self-hosted mailbox.

- **Owner-only.** This repository is private. The app is not published anywhere public:
  no Play Store listing, no public download link.
- Package: `net.sentinelsafeguard.app` (hunt line: `net.sentinelsafeguard.app.hunt`)
- Release signer certificate (SHA-256): `e30cf3915a9a8774025d04bf3625748219ca4b9ea67bf5779cbca0a8b1f1e28d`
  This fingerprint is the app's permanent signer identity. Updates verify against it.
- Design + scope: see the project spec held with the fleet records (Sentinel App SPEC,
  2026-10-25). v1 = Today · Tickets · Schedule · Mail · Clients.

No credentials, keys, or secrets live in this repository. Signing material is custodied
outside the repo and reaches CI only through GitHub Actions secrets.
