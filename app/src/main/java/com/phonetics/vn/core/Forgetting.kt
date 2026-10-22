package com.phonetics.vn.core

/**
 * Forgetting-curve maths. The user's "score that decays toward a half-life" is
 * exactly FSRS's model. FSRS-6 retrievability: R(t) = 1 / (1 + t / (9*S)), where S
 * is the item's stability (time over which recall drops to the target).
 */
object Forgetting {

    /** Recall probability after `elapsedDays` for an item of `stability` (days-ish). */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        if (stability <= 0.0) 0.0 else 1.0 / (1.0 + elapsedDays / (9.0 * stability))

    /** How many days until retrievability would fall to `targetRetention` (0..1). */
    fun daysToTarget(stability: Double, targetRetention: Double): Double {
        require(targetRetention > 0.0 && targetRetention < 1.0)
        return 9.0 * stability * (1.0 / targetRetention - 1.0)
    }
}
