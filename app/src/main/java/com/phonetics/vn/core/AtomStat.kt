package com.phonetics.vn.core

import kotlin.math.max as kmax
import kotlin.math.min as kmin

/** Per-atom learning state: raw fraction (for user/LLM display) + FSRS-ish decay. */
data class AtomStat(
    val atom: Atom,
    var attempts: Int = 0,
    var correct: Int = 0,
    var stability: Double = 2.0,  // "half-life" proxy in days-ish
    var difficulty: Double = 5.0, // 1..10, higher = harder
    var lastReviewMs: Long = 0L,
) {
    val successRate: Double get() = if (attempts == 0) 0.0 else correct.toDouble() / attempts

    /** Unsimplified fraction exactly as the user asked, e.g. "19/20". */
    val fraction: String get() = "$correct/$attempts"
}

/**
 * Updates atom stats with "massive-score insulation": weak atoms are affected most,
 * already-mastered atoms barely at all. Clear-diff wrong hits only implicated atoms;
 * ambiguous wrong shares blame equally across all atoms (per product decision).
 */
class AtomUpdater(
    private val gain: Double = 0.25,
    private val baseLoss: Double = 0.35,
    private val minLoss: Double = 0.03,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private fun need(a: AtomStat): Double = 1.0 - a.successRate.coerceIn(0.0, 1.0)

    fun onCorrect(a: AtomStat) {
        a.attempts++
        a.correct++
        val n = need(a)
        a.stability *= 1.0 + gain * (0.5 + 0.5 * n)
        a.difficulty = kmax(1.0, a.difficulty - 0.15 * n)
        a.lastReviewMs = clock()
    }

    /** Implicated-only wrong; insulated so mastered atoms are barely punished. */
    fun onWrong(a: AtomStat) {
        a.attempts++
        val n = need(a)
        val loss = kmax(minLoss, baseLoss * n)
        a.stability *= kmax(0.2, 1.0 - loss)
        a.difficulty = kmin(10.0, a.difficulty + 0.2 * n)
        a.lastReviewMs = clock()
    }

    /** Ambiguous wrong: uniform small decay across all atoms in the question. */
    fun onAmbiguousWrong(stats: Collection<AtomStat>, uniformLoss: Double = 0.06) {
        for (a in stats) {
            a.attempts++
            a.stability *= kmax(0.5, 1.0 - uniformLoss)
            a.lastReviewMs = clock()
        }
    }
}
