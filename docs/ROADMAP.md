# Phở-netics — Remaining Roadmap & Effort

> Status guide: ✅ done · 🟨 designed/planned · ⬜ not started
> Effort = rough focused-work sessions (a “session” ≈ a few hours of build + test cycles via CI).
> Honest note: the pipeline and shell are real today, but the **learning features are not built yet**.

## What exists now (committed)

- ✅ **M0** GitHub Actions build + release pipeline (compiles APK on GitHub cloud)
- ✅ **M0** app shell: launches, dark Vietnamese-flag theme, lists the 5 Southern tones
- ✅ **M1-core** Phase 1 HVPT **tone scheduler** engine (300–500 trials, ≥6 talkers, interleaving) + unit tests

---

## Phase A — Tone Trainer (doc Phase 1) · ~2–3 sessions · depends on audio pipeline

| Item | Size | Notes / deps |
|---|---|---|
| 🟨 Content pipeline (`tools/content/`) | small–med | Python script; VieNeu-TTS generates tone stimuli across ≥6 Southern voices; exports audio + `manifest.json` into assets. **You run once on a machine with Python + model** (CPU ok). |
| 🟨 Tone-trainer UI | med | Listen → pick 5-tone button → feedback → progress; session slicing (~40×10). |
| 🟨 Pitch-contour visualization | med | Mic capture + lightweight YIN F0 + draw your contour vs. model. |
| 🟨 Phase 0 onboarding demos | small–med | 5 interactive contrast demos (tone pair, word order, classifier, pronoun, particle). |

## Phase B — Vocabulary + Scheduler + Knownness (doc Phase 2) · ~3–4 sessions · biggest chunk

| Item | Size | Notes / deps |
|---|---|---|
| 🟨 Vocab data pack | med | Frequency-ordered top ~500 VN words + gloss + audio + illustration. **License audit before shipping (CC BY-SA attribution).** |
| 🟨 FSRS integration (FSRS-6 via FSRS-Kotlin) | med | Retention params 0.90 declarative / 0.85 production. |
| 🟨 BKT knownness + multi-dimension | med | Recognition/recall now; pronunciation/grammar dims later. |
| 🟨 Drill seam | med | pretest-MCQ, match, typed production; interleaving selector; 80/20 rule. |
| 🟨 Weak-point panel + progress | small–med | Rule-based (later LLM-assisted). |

## Phase C — LLM adaptation + Settings (self-adaptive path) · ~2 sessions

| Item | Size | Notes / deps |
|---|---|---|
| 🟨 Settings screen (API key) | small | DeepInfra/OpenAI-compatible: base URL + key + model + sync now. **Needs your key at this step.** |
| 🟨 Daily sync client | med | Compact stats out → session plan / 80-20 items / weak notes back; cached offline. |
| 🟨 Insights ledger + export | small–med | Store + share/upload; your feedback channel for updates. |

## Phase D — Grammar & pragmatics (doc Phases 3 + 6) · ~2 sessions

- Grammar crutch cards + production prompts (word order, classifiers, tense particles)
- Cloze / sentence-reconstruct drills (LLM-generated when synced)
- Pragmatic choice + role-play text chat (LLM)

## Phase E — Oral production + immersion (doc Phases 4 + 5) · ~3–4 sessions · HIGH RISK

- Gipformer ASR via sherpa-onnx Android AAR (pronunciation scoring, “mirror-pronounce”)
- Immersion: i+1 texts + comprehension; audio
- ⚠️ Native ML integration is an unproven spike — largest uncertainty in the whole project

## Phase F — Polish · variable / ongoing

- Real (non-debug) signing keystore, accent/a11y contrast pass, app icon refinement
- Real Southern talker recordings swapping in for synthetic stimuli (asset-only, doc-faithful)
- Crash/analytics (crashlytics or simple), Play-grade metadata (we are sideload-only for now)

---

## Effort summary (rough, honest)

| Target | Scope | Rough effort |
|---|---|---|
| Strong **v1.0** | A + B + C | ≈ **7–9 sessions** (~1.5–2 weeks focused) |
| v1.1 | + D (grammar/pragmatics) | ≈ 2 more |
| v2 | + E (oral/immersion) | ≈ 3–4 more · risky |
| Full doc vision | + F + v1.2 real talkers | ongoing |

## Decisions / inputs you must provide at these points

1. Run the `tools/content` audio generation once (need Python + the TTS model) — or approve using a prepackaged small pilot set I generate elsewhere.
2. Your **DeepInfra key + model name** at Phase C.
3. Approve the **vocab/gloss license choice** (open + attribution vs. hand-authored) before public ship.
4. Real-talker recordings (v1.2) are out of scope unless you source consenting Southern speakers.

## What I will NOT build unless you ask

iOS · app-store distribution · user accounts/cloud sync · PhoWhisper fallback · formal psychometric IRT calibration.
