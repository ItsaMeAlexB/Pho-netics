package com.phonetics.vn.core

/**
 * Question selection. Per product: show the prioritized components first, and within
 * them the item whose atoms are closest to forgetting (lowest FSRS retrievability).
 * Format escalates from recognition toward production as the item strengthens.
 */
class Scheduler(private val clock: () -> Long = { System.currentTimeMillis() }) {

    private fun statOr(stats: Map<Atom, AtomStat>, a: Atom): AtomStat = stats[a] ?: AtomStat(a)

    /** Mean retrievability across a question's atoms; never-seen atoms count as fully due. */
    fun meanRetrievability(q: Question, stats: Map<Atom, AtomStat>): Double {
        if (q.atoms.isEmpty()) return Forgetting.retrievability(0.0, 1.0)
        val now = clock()
        var sum = 0.0
        for (a in q.atoms) {
            val s = statOr(stats, a)
            if (s.lastReviewMs == 0L) continue // unseen: contributes 0 (fully due)
            val elapsed = ((now - s.lastReviewMs) / 86_400_000.0).coerceAtLeast(0.0)
            sum += Forgetting.retrievability(elapsed, s.stability)
        }
        return sum / q.atoms.size
    }

    /** Pick the next question: prioritized components only (else all enabled); lowest mean.
     *  retrievability first, stable tie-break by id. */
    fun next(questions: List<Question>, stats: Map<Atom, AtomStat>): Question? {
        val enabled = questions.filter { it.enabled }
        if (enabled.isEmpty()) return null
        val prioritized = enabled.filter { it.priority }
        val pool = if (prioritized.isNotEmpty()) prioritized else enabled
        return pool.minWithOrNull(
            compareBy<Question> { meanRetrievability(it, stats) }.thenBy { it.id },
        )
    }

    /** Recognition (tier 0) -> production (tier 3) as the item's fraction strengthens. */
    fun suggestedTier(q: Question, stats: Map<Atom, AtomStat>): Int {
        val meanFrac =
            if (q.atoms.isEmpty()) statOr(stats, placeholder(q)).successRate
            else q.atoms.map { statOr(stats, it).successRate }.average()
        return when {
            meanFrac >= 0.85 -> (q.tier + 1).coerceAtMost(3)
            meanFrac >= 0.6 -> q.tier.coerceAtLeast(1)
            else -> q.tier
        }
    }

    private fun placeholder(q: Question) = Atom(AtomKind.VOCAB, q.id, q.component)
}
