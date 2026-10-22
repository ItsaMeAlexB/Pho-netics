package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionEngineTest {

    private fun engine(withBank: Boolean = true): Pair<QuestionEngine, LearnerState> {
        val state = LearnerState()
        if (withBank) state.seedFromBank()
        return QuestionEngine(state) to state
    }

    @Test
    fun correctAnswerRewardsAllAtoms() {
        val (eng, state) = engine()
        val q = state.questions["s02"]!! // target "ba"
        val res = eng.submit(q, "ba")
        assertTrue(res.diagnosis.correct)
        for (a in q.atoms) {
            val s = state.stats[a]!!
            assertEquals(1, s.attempts)
            assertEquals(1, s.correct)
        }
    }

    @Test
    fun toneMissBlamesOnlyToneAtom() {
        val (eng, state) = engine()
        val q = state.questions["s09"]!! // target "má" (tone)
        val res = eng.submit(q, "mạ", fromSpeech = true)
        assertFalse(res.diagnosis.correct)
        assertTrue(res.diagnosis.failures.any { it.kind == AtomKind.TONE })
        // The tone atom took the hit; no vocab atom in "má" beyond tone.
        val toneAtom = q.atoms.firstOrNull { it.kind == AtomKind.TONE }
        assertTrue(toneAtom != null)
        assertEquals(1, state.stats[toneAtom]!!.attempts)
        assertEquals(0, state.stats[toneAtom]!!.correct)
    }

    @Test
    fun nonIsolatedWrongFallsBackToEqualShareBlame() {
        // A question whose only atom can't be isolated by the diff ("zzz" vs "con chó to"
        // blames GRAMMAR("con"), which doesn't match our custom GRAMMAR atom) -> equal share.
        val state = LearnerState()
        val comp = Component(KnowledgeGroup.VOCABULARY, "test")
        val a = Atom(AtomKind.GRAMMAR, "marker-x", comp)
        val q = Question("x", comp, Format.CLOZE, 0, "p", listOf("con chó to"), atoms = setOf(a))
        state.questions[q.id] = q
        val eng = QuestionEngine(state)
        val res = eng.submit(q, "zzz")
        assertFalse(res.diagnosis.correct)
        // Fallback applied uniform blame to the only atom.
        assertEquals(1, state.stats[a]!!.attempts)
        assertEquals(0, state.stats[a]!!.correct)
    }

    @Test
    fun nextReturnsEnabledQuestionOrNull() {
        val (eng, state) = engine()
        assertTrue(eng.next() != null)
        for (id in state.questions.keys) state.questions[id] = state.questions[id]!!.copy(enabled = false)
        assertEquals(null, eng.next())
    }

    private fun matchQuestion(): Pair<QuestionEngine, LearnerState> {
        val state = LearnerState()
        val comp = Component(KnowledgeGroup.VOCABULARY, "food-drink")
        val pairs = listOf("phở" to "noodle soup", "nước" to "water", "cà phê" to "coffee")
        val q = Question("m", comp, Format.MATCH, 0, "p", answers = pairs.map { it.first },
            atoms = AtomParser.atomsFor("phở nước cà phê", comp), matchPairs = pairs)
        state.questions[q.id] = q
        return QuestionEngine(state) to state
    }

    @Test
    fun matchAllPairsCorrectIsRewarded() {
        val (eng, state) = matchQuestion()
        val q = state.questions["m"]!!
        val res = eng.submitMatch(q, q.matchPairs)
        assertTrue(res.diagnosis.correct)
        for (a in q.atoms) assertEquals(1, state.stats[a]!!.correct)
    }

    @Test
    fun acceptedAnswerVariantCountsAsCorrect() {
        val (eng, state) = engine()
        val q = state.questions["s28"]!! // market / to the market
        val res = eng.submit(q, "to the market")
        assertTrue(res.diagnosis.correct)
        for (a in q.atoms) assertEquals(1, state.stats[a]!!.correct)
    }

    @Test
    fun unacceptedParaphraseIsStillWrong() {
        val (eng, state) = engine()
        val q = state.questions["s28"]!!
        val res = eng.submit(q, "he is at the cafe")
        assertFalse(res.diagnosis.correct)
        assertTrue(state.stats[q.atoms.first()]!!.attempts == 1)
    }
    @Test
    fun laterCorrectRetryDoesNotNegateFirstWrong() {
        val (eng, state) = engine()
        val q = state.questions["s02"]!! // ba
        eng.submit(q, "wrong")
        val a = q.atoms.first { it.key == "ba" }
        assertEquals(1, state.stats[a]?.attempts)
        eng.submit(q, "ba") // correct retry on the same card
        assertEquals(2, state.stats[a]?.attempts)
        assertEquals(1, state.stats[a]?.correct) // first wrong attempt is not erased
    }

    @Test
    fun skipCountsAsWrongFirstAttempt() {
        val (eng, state) = engine()
        val q = state.questions["s02"]!! // ba
        val res = eng.submit(q, "") // skip => treated as a wrong attempt
        assertFalse(res.diagnosis.correct)
        val a = q.atoms.first { it.key == "ba" }
        assertEquals(1, state.stats[a]?.attempts)
        assertEquals(0, state.stats[a]?.correct)
    }


    @Test
    fun matchWrongPairBlamesOnlyThatWord() {

        val (eng, state) = matchQuestion()
        val q = state.questions["m"]!!
        val wrong = listOf("phở" to "water", "nước" to "noodle soup", "cà phê" to "coffee")
        val res = eng.submitMatch(q, wrong)
        assertFalse(res.diagnosis.correct)
        assertTrue(res.diagnosis.failures.map { it.key }.toSet().containsAll(listOf("phở", "nước")))
        // Only the two mis-paired words took a hit (atoms are per whitespace token).
        assertEquals(1, state.stats[q.atoms.first { it.kind == AtomKind.VOCAB && it.key == "phở" }]!!.attempts)
        assertEquals(1, state.stats[q.atoms.first { it.kind == AtomKind.VOCAB && it.key == "nước" }]!!.attempts)
        // The correctly-paired word's tokens ("cà", "phê") were protected: no stats created.
        assertNull(state.stats[q.atoms.first { it.kind == AtomKind.VOCAB && it.key == "cà" }])
        assertNull(state.stats[q.atoms.first { it.kind == AtomKind.VOCAB && it.key == "phê" }])
    }
}
