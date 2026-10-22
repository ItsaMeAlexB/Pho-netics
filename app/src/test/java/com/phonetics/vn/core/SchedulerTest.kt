package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val comp = Component(KnowledgeGroup.VOCABULARY, "test")
    private val now = 1_000_000_000L
    private val sched = Scheduler(clock = { now })

    private fun q(id: String, atom: Atom, tier: Int = 0, priority: Boolean = true) =
        Question(id, comp, Format.FLASHCARD, tier, "p-$id", listOf("x"), atoms = setOf(atom), priority = priority)

    @Test
    fun unseenQuestionIsChosenOverRecentlyMastered() {
        val a1 = Atom(AtomKind.VOCAB, "x", comp)
        val a2 = Atom(AtomKind.VOCAB, "y", comp)
        val q1 = q("a", a1)
        val q2 = q("b", a2)
        val stats = mapOf(a1 to AtomStat(a1, attempts = 10, correct = 10, stability = 30.0, lastReviewMs = now))
        assertEquals("b", sched.next(listOf(q1, q2), stats)?.id)
    }

    @Test
    fun tieBetweenUnseenBreaksById() {
        val q1 = q("a", Atom(AtomKind.VOCAB, "x", comp))
        val q2 = q("b", Atom(AtomKind.VOCAB, "y", comp))
        assertEquals("a", sched.next(listOf(q1, q2), emptyMap())?.id)
    }

    @Test
    fun prioritizedComponentWinsOverUnprioritized() {
        val on = q("pri", Atom(AtomKind.VOCAB, "x", comp), priority = true)
        val off = q("off", Atom(AtomKind.VOCAB, "y", comp), priority = false)
        assertEquals("pri", sched.next(listOf(off, on), emptyMap())?.id)
    }

    @Test
    fun escalatesTierWithStrongFraction() {
        val a = Atom(AtomKind.VOCAB, "x", comp)
        val sq = sched.suggestedTier(q("c", a, tier = 1), mapOf(a to AtomStat(a, attempts = 20, correct = 19, stability = 30.0, lastReviewMs = now)))
        assertEquals(2, sq)
    }

    @Test
    fun tierIsCappedAtProduction() {
        val a = Atom(AtomKind.VOCAB, "x", comp)
        val sq = sched.suggestedTier(q("d", a, tier = 3), mapOf(a to AtomStat(a, attempts = 20, correct = 20, stability = 30.0, lastReviewMs = now)))
        assertEquals(3, sq)
        assertTrue(sq <= 3)
    }
}
