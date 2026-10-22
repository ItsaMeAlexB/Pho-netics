# Phở-netics — Architecture & Continuity Reference

> **Read this file first in any fresh session.** It is the authoritative, current
> snapshot of what this project is, the final product decisions, what's already built,
> and how to build/test/ship it — so work can continue even if the original planning
> chat is gone. It supersedes the earlier `docs/ROADMAP.md` where the two disagree.

## 1. What this is

A neuroscience-grounded **Southern Vietnamese** learning app for English speakers, built
from the design spec in `Phở-netics.dotx` (contrastive analysis of English vs Vietnamese,
the Declarative/Procedural model, spacing/retrieval/pretesting evidence, i+1 input, pushed
output, pragmatics). The product is a **question engine**, not a fixed phrasebook: a
self-adapting stack of questions whose difficulty, format, and content change with the
learner, with an LLM that manages content and priorities in the background.

## 2. Final product & non-negotiable constraints (user decisions — do not regress)

- **App boot:** opens straight into a single-question stack (one card at a time). Audio
  plays on a 🔊 press; speaking uses a 🎤 record button.
- **Question formats:** flashcards/MCQ, match, type, cloze, audio, oral — escalating from
  recognition toward production as an item's score rises.
- **Fully on-device speech** (both STT and TTS). **Nothing personal ever leaves the
  phone: no mic audio, no personally identifying info.** Only **text-only, anonymous
  LLM sync** is allowed (see §7).
- **DeepSeek official** (cheaper) is the LLM provider — **not** DeepInfra — so the app
  must not depend on any DeepInfra-only endpoint (no cloud TTS/ASR via DeepInfra).
- **APK size is not a constraint.** Bundling local speech models (~370–420MB) is fine.
- One clean, complete download — no incrementally-updated APK.

## 3. Current status (what is real today — committed and locally green)

Git history has these commits, all compile + pass 42 unit tests via the local toolchain:

- `54b15e5` M0 skeleton + GitHub Actions build/release pipeline
- `052d35c` dark Vietnamese-flag theme + adaptive icon
- `9ef4a4c` ToneCurriculum (Phase 1 HVPT tone scheduler)
- `2751dc9` engine: Taxonomy/Atom/Question/Forgetting/AtomStat/updater/AnswerDiffer
- `13e2cc0` content: AtomParser + SeedBank (22 Qs) + Scheduler
- `6447430` SyncScheduler (DeepSeek off-peak Beijing)
- `3b79c79` docs/ROADMAP.md
- `6cf0d9a` fix first real compile error (Int vs Byte) + docs/TOOLCHAIN.md + gitignore

CI (`build.yml`) runs the same unit tests + builds a debug APK on push. `release.yml`
attaches the APK to a GitHub Release on a `v*` tag.

## 4. Data model (package `com.phonetics.vn.core`)

| File | Purpose |
|---|---|
| `Taxonomy.kt` | `KnowledgeGroup` (PRONUNCIATION/VOCABULARY/GRAMMAR/PRAGMATICS), `Component(group, subgroup)`, `seedComponents()` |
| `Atom.kt` | `AtomKind` (VOCAB/SPELLING/SOUND/TONE/SYNTAX/GRAMMAR/PRAGMATICS), `Atom(kind, key, component)` |
| `Question.kt` | `Format` (FLASHCARD/MATCH/TYPE/CLOZE/AUDIO/ORAL), `Question(id, component, format, tier, prompt, answers, hasAudio, needsMic, atoms, priority, enabled)` |
| `Forgetting.kt` | FSRS-6: `retrievability(t,S) = 1/(1 + t/(9S))`, `daysToTarget(...)` |
| `AtomStat.kt` | per-atom `attempts/correct` (unsimplified fraction like `19/20`), `stability`, `difficulty`, `lastReviewMs`, `successRate`; `AtomUpdater` (insulation + equal-share) |
| `AnswerDiffer.kt` | diagnostic diff engine (tone/spelling/sound/vocab/syntax/grammar/pragmatics) |
| `AtomParser.kt` | derives atoms locally from answer text (never trusts LLM tags) |
| `SeedBank.kt` | 22 starter questions across the taxonomy |
| `Scheduler.kt` | selection (priority-filtered, lowest mean retrievability) + `suggestedTier` escalation |
| `ToneCurriculum.kt` | Phase 1 HVPT: deterministic 300–500 tone-ID trials, ≥6 talkers, interleaving |
| `SyncScheduler.kt` | picks DeepSeek off-peak (Beijing) windows |

### Scoring
- Each **atom** keeps a raw unsimplified fraction (`correct/attempts`, shown to both the
  user and the LLM) and an **FSRS-style stability** that decays over time — the
  "half-life" forgetting maths.
- **Insulated updates:** a miss mostly hurts weak atoms; already-mastered atoms barely
  move (their high successRate means a small `need`, hence a small penalty).
- **Equal-share fallback:** if the diff can't isolate the bad atom, all of the question's
  atoms take one uniform small loss — no cloud round-trip.
- **Multi-component questions:** a sentence touches many words + a grammar rule + tones;
  only the **implicated** atoms get adjusted, so known words in a miss are protected.

### The diff engine (`AnswerDiffer`)
Vietnamese orthography writes **base letters = sounds** and **diacritics = tone/vowel
quality**, so string ops classify errors for free: same base letters but different
diacritics ⇒ TONE; homophone/near-miss (§) ⇒ SPELLING (typed) or SOUND (spoken); whole-word
change ⇒ VOCAB; function/classifier/particle missing ⇒ GRAMMAR or PRAGMATICS; reordering /
omission ⇒ SYNTAX. Works on typed answers **and** on ASR transcripts.

<!-- PART1_END -->

## 5. Remaining work (build in this order; compile-verify each layer locally)

1. **Persistence** — `LearnerStore` (SharedPreferences + org.json): questions, stats,
   flags, priorities, settings survive relaunch. Serialize atoms via
   `kind|group|subgroup|key`.
2. **Question-stack UI** — Compose screen that boots to the stack, renders each format,
   submits → `AnswerDiffer` → `AtomUpdater` → advance via `Scheduler.next`; show feedback;
   allow flag-with-note. Rewire `MainActivity` around it.
3. **Settings + scores** — API provider/base-url/model/key (masked), sync off-peak cadence
   + on/off, progress shown as unsimplified fractions per group/subgroup, manual priority
   overrides, theme (dark / light / flag).
4. **DeepSeek sync client** — OpenAI-compatible HTTP (`https://api.deepseek.com`, chat
   completions only), scheduled at `SyncScheduler.nextOffPeak`, sends compact aggregate
   fractions + deltas, applies returned structured actions to the bank. Text-only.
5. **Native speech** (feature-flagged) — see §8; bundle on-device STT + TTS behind a flag
   so the app is complete and functional either way.

## 6. How to build & test

Local (this machine has the toolchain): follow `docs/TOOLCHAIN.md` — set `JAVA_HOME` to
`.toolchain/jdk`, then `.toolchain/gradle-8.11.1/bin/gradle test` (unit tests) or
`... assembleDebug` (APK). `local.properties` already points `sdk.dir` at the local SDK.

CI (GitHub Actions): `build.yml` runs tests + builds the debug APK on every push/PR;
`release.yml` attaches `phonetics-<ver>.apk` to a Release when you push a `v*` tag. The
phone installs the one green Release APK. (Debug-signed by default for personal use;
real keystore via repo secrets is documented in `README.md`.)

## 7. LLM sync contract (text-only; the only thing that ever leaves the device)

**Out (compact, anonymous):** per (group, subgroup) → `frac_prev` and `frac_now` as
unsimplified fractions such as `3/20`, `19/20`, plus question count, flagged-question
notes, and a snapshot of the weakest groups. Never raw answer text, never voice, never
any personally identifying info, never a transcript of anything the learner said.

**In (structured actions, JSON):** prioritize/demote groups, create questions, update
questions (format/tier), delete questions, reclassify questions (e.g. move syntax →
Grammar), add new components, and optionally a `review_request` for a group — the *next*
sync answers with that group's full question+score list so the LLM can audit it.

**Local-first:** knowledge atoms are always derived locally (`AtomParser`); the LLM's own
labels are advisory only and never gate or override. Only prioritized groups/subgroups
appear in the stack; the user can override priorities manually in Settings.

## 8. On-device speech (verified research)

- **STT** (pronunciation scoring, incl. tone — Vietnamese orthography reveals tone in the
  transcript's diacritics): **`phostt`** (`ekhodzitsky/phostt`) — Rust, `libphostt.so` via
  cargo-ndk + JNI/Kotlin bridge; uses sherpa-onnx Zipformer-vi RNNT weights (~75MB ONNX,
  INT8); ~90–100MB total. ⚠️ Verify its license before shipping.
- **TTS** (Southern, text→speech for new content): **`VieNeu-TTS v3 Nano`** — **Apache-2.0**
  (covers models, bundled preset voices, and generated audio; commercial OK), ONNX
  torch-free CPU, but **~280MB of ONNX graphs**, faster-than-realtime, and a *preview*
  with known quality gaps. ⚠️ Its exact preset-voice roster isn't confirmed Southern —
  verify before relying on it; a Piper-VITS Vietnamese voice via sherpa-onnx is a lighter
  fallback. (The 0.3B-GGUF variant needs a Python/llama.cpp runtime — avoid; Nano is the
  ONNX/Android-friendly one.)
- **`Gipformer`** (MIT, Zipformer-RNNT, ONNX/mobile, benchmarked incl. Southern) is a
  high-accuracy ASR alternative if `phostt`'s license turns out to be a problem.

## 9. Key verified reference facts

- **DeepSeek:** OpenAI-compatible chat at `https://api.deepseek.com`; **chat only — no
  speech endpoint**, so all speech must be on-device. Off-peak pricing (since Aug 2026):
  Beijing **09:00–12:00 and 14:00–18:00 weekdays are peak**; every other hour, all weekends,
  and Chinese public holidays are **50% off**.
- **FSRS:** use FSRS-6 (via FSRS-Kotlin); "FSRS-7" is not released in any library yet.
- **Southern tones:** 5 phonemic (ngã and hỏi merge) — ngang, huyền, sắc, hỏi, nặng.

## 10. How to hand off to a fresh session

1. Read this file.
2. On this machine: `cd /home/alex/Documents/Phở-netics`, follow `docs/TOOLCHAIN.md`
   (`gradle test`) to re-confirm green before changing UI/persistence.
3. Continue §5 in order, compiling after each layer.
4. Keep the privacy constraints (§2) and text-only sync contract (§7) sacred — never
   send mic audio or PII to any cloud.
