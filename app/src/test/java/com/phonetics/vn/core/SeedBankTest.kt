package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedBankTest {

    @Test
    fun bankIsPopulatedAcrossTaxonomy() {
        val qs = SeedBank.questions()
        assertTrue(qs.size >= 20)
        assertEquals(qs.size, qs.map { it.id }.toSet().size) // unique ids
        assertTrue(qs.map { it.component }.toSet().size >= 3) // spread across groups
    }

    @Test
    fun everyQuestionHasLocallyDerivedAtoms() {
        for (q in SeedBank.questions()) {
            assertTrue("question ${q.id} has no atoms", q.atoms.isNotEmpty())
        }
    }

    @Test
    fun componentsBelongToSeededTaxonomy() {
        val seeded = seedComponents().toSet()
        for (q in SeedBank.questions()) {
            assertTrue("${q.component} not seeded", q.component in seeded)
        }
    }

    @Test
    fun formatMediaFlagsAreConsistent() {
        for (q in SeedBank.questions()) {
            if (q.format == Format.ORAL) assertTrue(q.needsMic)
            if (q.format == Format.AUDIO) assertTrue(q.hasAudio)
        }
    }

    @Test
    fun matchQuestionsAreTwoSidedPairs() {
        val match = SeedBank.questions().filter { it.format == Format.MATCH }
        assertTrue(match.isNotEmpty())
        for (m in match) {
            assertTrue("match ${m.id} has no pairs", m.matchPairs.isNotEmpty())
            // every pair has a Vietnamese side and an English meaning.
            m.matchPairs.forEach { (vn, en) ->
                assertTrue(vn.isNotBlank())
                assertTrue(en.isNotBlank())
            }
        }
    }

    @Test
    fun paragraphQuestionsPresentAndAreAnswerable() {
        val para = SeedBank.questions().filter { it.format == Format.PARAGRAPH }
        assertTrue(para.size >= 3)
        for (p in para) {
            assertTrue(p.atoms.isNotEmpty())
            assertTrue(p.targetText.isNotBlank())
        }
    }

    @Test
    fun someCardsCarryEmojiDualCodingCues() {
        assertTrue(SeedBank.questions().any { it.emoji.isNotBlank() })
    }
}
