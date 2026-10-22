# Phở-netics

A neuroscience-grounded **Southern Vietnamese** learning app for English speakers, built
from the design spec in this repo (`Phở-netics.dotx`).

**Status:** the full learning engine is built and green (42 unit tests) — see
`docs/ARCHITECTURE.md` for the complete, up-to-date architecture and decisions. Still to
build: the question-stack UI, persistence, the DeepSeek sync client, and on-device
speech (all documented there).

> **Continuity:** in any new session, **read `docs/ARCHITECTURE.md` first** — it captures
> the product decisions, private/security constraints, research facts, and how to continue
> without the original planning chat. `docs/TOOLCHAIN.md` has the local-build commands;
> `docs/ROADMAP.md` was the earlier plan (superseded where the two differ).

## What it will be (the science-backed roadmap)

| Release | Contents |
|---|---|
| **v1.0** | Phase 0 onboarding · Phase 1 HVPT tone trainer (5 Southern tones, pitch viz) · Phase 2 high-frequency vocab (MCQ pretest → FSRS → production) · optional daily LLM sync + insights ledger |
| v1.1 | Phase 3 grammar crutches + Phase 6 text role-plays (via LLM) |
| v1.2 | Real Southern talkers swapping in for synthetic stimuli (asset-only change) |
| v2 | Phase 4 oral practice (Gipformer ONNX via sherpa-onnx, pronunciation scoring) + Phase 5 immersion |

Adaptation is a **fast loop** (in-app: FSRS/BKT scheduling + LLM-driven learning-path
changes — data only, never code) plus a **slow loop** (anonymized insights ledger you
review, then ship as new releases/content packs).

## Getting the APK on your phone

1. Open the **Releases** page of this repository (or use the latest `Build` workflow
   artifact).
2. Download the `.apk` file.
3. Open it on your phone → allow "install from unknown sources" once → install.
4. Run **Phở-netics**. (M0 shows the app shell + the 5 Southern tones as a sanity check.)

> ⚠️ The v0.x APK is **debug-signed** for convenience (installable, but intended for
> personal use). A proper release keystore is documented below.

## Building

**Two ways to build.**

**Via GitHub Actions (no local SDK needed):** every push runs `build.yml` (unit tests +
debug APK), and a `v*` tag runs `release.yml` to attach the APK to a GitHub Release.

**Locally** (needs JDK 17 + Android SDK + Gradle 8.11.1). This machine already has a local
toolchain — follow `docs/TOOLCHAIN.md` for the exact commands:

```bash
export JAVA_HOME=$PWD/.toolchain/jdk
export PATH=$JAVA_HOME/bin:$PATH
.toolchain/gradle-8.11.1/bin/gradle test --console=plain --no-daemon          # unit tests
.toolchain/gradle-8.11.1/bin/gradle assembleDebug --console=plain --no-daemon # APK
```

## Project layout

```
app/                      # Kotlin + Jetpack Compose (minSdk 26)
app/src/main/java/com/phonetics/vn/
  core/                   # domain models (tones, later: scheduler, knownness)
  MainActivity.kt         # app shell (M0)
content/sources/          # (M1+) vocab CSV, tone-sets JSON, CREDITS
tools/content/            # (M1+) Python: VieNeu-TTS stimulus generation
.github/workflows/
  build.yml               # tests + debug APK on every push/PR
  release.yml             # tag v* → GitHub Release with signed APK
```

## Shipping a new APK to your phone

Tag a release and the `Release` workflow attaches an installable APK to it:

```bash
git tag v0.1.0
git push origin v0.1.0
```

Then install from the repo's Releases page.

### Adding a real (non-debug) signing key (optional, for wider distribution)

1. Generate a keystore locally and add these repo **secrets**:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Uncomment/extend the signing config in `app/build.gradle.kts` to read them,
   and switch `release.yml` to `assembleRelease`.
3. Keep the keystore themselves **out of the repo** (already gitignored).

## Technical decisions (verified)

- **Framework:** Kotlin + Jetpack Compose (native, no iOS needed).
- **LLM:** official **DeepSeek** (OpenAI-compatible, chat-only, `https://api.deepseek.com`) —
  optional, per-user key, offline-first; text-only (no voice, no PII).
- **Speech (on-device):** `phostt` (STT) + `VieNeu-TTS v3 Nano` (TTS) — see
  `docs/ARCHITECTURE.md` §8 for the verified research and licensing caveats.
- **Scheduler:** FSRS-6 (FSRS-Kotlin); doc's "FSRS-7" isn't in a released library yet.

## Content & licensing

Derived content (word glosses, frequency lists) will be audited before shipping and
credited here. Tone stimuli are generated synthetically at build time; a real-talker
swap is the v1.2 goal (HVPT literature prefers real talkers).

## Privacy

Mic audio never leaves the device. Opt-in sync sends only compact interaction stats to
the endpoint **you** configure (default: none).

## Roadmap notes

- M1: content pipeline + Phase 0 + Phase 1 tone trainer
- M2: Phase 2 vocab + FSRS + BKT knownness
- M2.5: optional daily LLM sync + insights ledger
- v1.1+: grammar + role-plays; v2: oral practice (ASR)

---

*Built from the `Phở-netics.dotx` systematic design. License: see `LICENSE`.*
