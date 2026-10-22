package com.phonetics.vn.core

import kotlin.random.Random

/**
 * One HVPT tone-identification trial: the learner hears a stimulus and must pick
 * which of the 5 Southern tones it carries.
 */
data class ToneTrial(
    /** Sequential id across the whole curriculum (1-based), stable for offline content. */
    val id: Int,
    /** Carrier syllable or word the stimulus is built from (e.g. "ma"). */
    val carrierId: String,
    /** Voice/talker id (HVPT evidence requires >= 6 distinct talkers). */
    val talkerId: String,
    /** Correct answer: 1-based key into [VietTones.SOUTHERN] (1..5). */
    val correctToneIndex: Int,
) {
    val correctTone: Tone get() = VietTones.SOUTHERN[correctToneIndex - 1]
}

/**
 * Builds the Phase 1 HVPT tone-identification curriculum.
 *
 * Evidence (doc Part IV): adults learn novel pitch categories after a few hundred
 * trials; identification beats discrimination; trial-by-trial feedback with pitch
 * visualization helps; talker variability is a key moderator. So we schedule
 * ~300-500 identification trials across >=6 talkers, interleaved so the same talker
 * or tone never repeats back to back.
 *
 * Deterministic for a given [seed] so the generated content plan is reproducible.
 * Session slicing (doc: ~40 trials x 10 sessions) is the caller's concern.
 */
object ToneCurriculum {

    fun plan(
        carriers: List<String>,
        talkers: List<String>,
        totalTrials: Int = 400,
        seed: Int = 7,
        rng: Random = Random(seed),
    ): List<ToneTrial> {
        require(carriers.isNotEmpty()) { "need at least one carrier syllable" }
        require(talkers.size >= 6) { "HVPT needs >= 6 talkers, got ${talkers.size}" }

        val tones = VietTones.SOUTHERN

        // Full (carrier x tone x talker) stimulus pool.
        val pool =
            buildList {
                for (c in carriers) for (t in tones) for (tk in talkers) add(Triple(c, t, tk))
            }

        val out = ArrayList<ToneTrial>(totalTrials)
        var lastTalker: String? = null
        var lastTone = 0
        while (out.size < totalTrials) {
            // Prefer candidates differing in BOTH talker and tone (interleaving).
            val fresh =
                pool.filter { (_, t, tk) -> tk != lastTalker && t.index != lastTone }
            val pick =
                if (fresh.isEmpty()) pool[rng.nextInt(pool.size)]
                else fresh[rng.nextInt(fresh.size)]
            out += ToneTrial(out.size + 1, pick.first, pick.third, pick.second.index)
            lastTalker = pick.third
            lastTone = pick.second.index
        }
        return out
    }
}
