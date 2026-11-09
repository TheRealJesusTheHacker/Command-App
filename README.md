# Command App

Private Android app for **Sentinel Command** and **Sentinel Mail** — the owner's phone
front-end for Sentinel Safeguard's private command system and self-hosted mailbox.

- **Owner-only.** This repository is private. The app is not published anywhere public:
  no Play Store listing, no public download link.
- Package: `net.sentinelsafeguard.commandapp` (hunt line: `net.sentinelsafeguard.commandapp.hunt`)
- Release signer certificate (SHA-256): `e30cf3915a9a8774025d04bf3625748219ca4b9ea67bf5779cbca0a8b1f1e28d`
  This fingerprint is the app's permanent signer identity. Updates verify against it.
- Design + scope: see the project spec held with the fleet records (Command App SPEC — originally the Sentinel App SPEC,
  2026-10-25; the app was renamed completely to Command App on 2026-11-09, repo and all:
  this repository was TheRealJesusTheHacker/Sentinel-App until the rename). v1 = Today · Tickets · Schedule · Mail · Clients.
- **Mirror program (hunt5, 2026-11-02):** the app is being rebuilt as a native
  view-for-view mirror of browser Sentinel Command (COMMAND-MIRROR-SPEC): same
  navigation groups, views, data, and actions — no WebView surfaces. hunt5 lands
  the mirrored shell plus Today, Dashboard, Tickets, and Clients/Pipeline;
  unbuilt Command views appear under their exact names with a placeholder
  naming the phase they land in.
- **Mirror Phase 2 (hunt6, 2026-11-03):** Cases, the full Schedule mirror
  (Month / Week / People / Agenda, job writes, schedule entries), Engagements,
  Runbooks & Sign-off (including the ticket-profile sign-off surface),
  Today's injected panels, and global search land as native mirrors on the
  Phase 2 backend; ticket rows now carry their case title. Remaining
  placeholders name Phases 3–9.

No credentials, keys, or secrets live in this repository. Signing material is custodied
outside the repo and reaches CI only through GitHub Actions secrets.
