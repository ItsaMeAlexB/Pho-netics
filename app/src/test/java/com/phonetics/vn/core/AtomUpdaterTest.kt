package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomUpdaterTest {

    private val atom = Atom(AtomKind.VOCAB, "chó", Component(KnowledgeGroup.VOCABULARY, "animals"))

    @Test
    fun correctGrowsStabilityAndBothCounters() {
        val up = AtomUpdater(clock = { 1L })
        val st = AtomStat(atom)
        up.onCorrect(st)
        val s0 = st.stability
        up.onCorrect(st)
        assertEquals(2, st.attempts)
        assertEquals(2, st.correct)
        assertTrue(st.stability > s0)
    }

    @Test
    fun fractionIsUnsimplified() {
        val st = AtomStat(atom).apply { correct = 19; attempts = 20 }
        assertEquals("19/20", st.fraction)
    }

    @Test
    fun masteredAtomIsBarelyPunishedOnWrong() {
        val up = AtomUpdater(clock = { 1L })
        val st = AtomStat(atom).apply { correct = 38; attempts = 40 }
        val before = st.stability
        up.onWrong(st)
        assertTrue(st.stability <= before)
        assertTrue((before - st.stability) / before < 0.2)
    }

    @Test
    fun weakAtomLossIsLargerThanMastered() {
        val up = AtomUpdater(clock = { 1L })
        val weak = AtomStat(atom)
        val strong = AtomStat(atom).apply { correct = 19; attempts = 20 }
        up.onWrong(weak)
        up.onWrong(strong)
        assertTrue((2.0 - weak.stability) / 2.0 > (2.0 - strong.stability) / 2.0)
    }

    @Test
    fun ambiguousWrongSharesBlameEquallyAndNoDifficultySpike() {
        val up = AtomUpdater(clock = { 1L })
        val a = AtomStat(atom)
        val b = AtomStat(atom.copy(key = "b"))
        val dBefore = a.difficulty
        up.onAmbiguousWrong(listOf(a, b))
        assertEquals(1.0, a.stability / b.stability, 1e-9) // both scaled identically
        assertEquals(0.94, a.stability / 2.0, 1e-9)       // default stability 2.0, loss 0.06
        assertEquals(dBefore, a.difficulty, 1e-9)
    }
}
