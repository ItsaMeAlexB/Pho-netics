package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerDifferTest {

    private val differ = AnswerDiffer()

    @Test
    fun identicalAnswerIsCorrect() {
        val d = differ.analyze("con chó to", "con chó to", fromSpeech = false)
        assertTrue(d.correct)
        assertTrue(d.failures.isEmpty())
    }

    @Test
    fun toneOnlyThingWrongBlamesTone() {
        val d = differ.analyze("mạ", "má", fromSpeech = true)
        assertFalse(d.correct)
        assertEquals(listOf(AtomKind.TONE), d.failures.map { it.kind })
    }

    @Test
    fun typedTypoBlamesSpelling() {
        val d = differ.analyze("sinh", "xinh", fromSpeech = false)
        assertEquals(listOf(AtomKind.SPELLING), d.failures.map { it.kind })
    }

    @Test
    fun spokenNearMissBlamesSound() {
        val d = differ.analyze("sinh", "xinh", fromSpeech = true)
        assertEquals(listOf(AtomKind.SOUND), d.failures.map { it.kind })
    }

    @Test
    fun differentWordBlamesVocab() {
        val d = differ.analyze("chó", "mèo", fromSpeech = false)
        assertEquals(listOf(AtomKind.VOCAB), d.failures.map { it.kind })
    }

    @Test
    fun missingPolitenessMarkerBlamesPragmatics() {
        val d = differ.analyze("cảm ơn ạ", "cảm ơn", fromSpeech = false)
        assertEquals(listOf(AtomKind.PRAGMATICS), d.failures.map { it.kind })
    }

    @Test
    fun reorderingBlamesSyntax() {
        val d = differ.analyze("con chó to", "con to chó", fromSpeech = false)
        assertEquals(AtomKind.SYNTAX, d.failures.single().kind)
    }

    @Test
    fun missingTenseParticleBlamesGrammar() {
        val d = differ.analyze("tôi đã ăn", "tôi ăn", fromSpeech = false)
        assertTrue(d.failures.any { it.kind == AtomKind.GRAMMAR })
    }
}
