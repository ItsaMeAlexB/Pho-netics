package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomParserTest {

    private val vocab = Component(KnowledgeGroup.VOCABULARY, "greetings")
    private val grammar = Component(KnowledgeGroup.GRAMMAR, "tense-particles")

    @Test
    fun derivesVocabToneAndPragmatics() {
        val atoms = AtomParser.atomsFor("cảm ơn ạ", vocab)
        val kinds = atoms.map { it.kind }.toSet()
        assertTrue(AtomKind.VOCAB in kinds)   // cảm, ơn
        assertTrue(AtomKind.TONE in kinds)    // diacritics present
        assertTrue(AtomKind.PRAGMATICS in kinds) // ạ
    }

    @Test
    fun derivesGrammarForFunctionWords() {
        val atoms = AtomParser.atomsFor("tôi đã ăn", grammar)
        assertTrue(atoms.any { it.kind == AtomKind.GRAMMAR && it.key == "đã" })
    }

    @Test
    fun classifierIsGrammar() {
        val atoms = AtomParser.atomsFor("con chó to", Component(KnowledgeGroup.GRAMMAR, "classifiers"))
        assertTrue(atoms.any { it.kind == AtomKind.GRAMMAR && it.key == "con" })
    }

    @Test
    fun distinctTokensBecomeDistinctVocabAtoms() {
        val atoms = AtomParser.atomsFor("phở nước", vocab)
        val vocabKeys = atoms.filter { it.kind == AtomKind.VOCAB }.map { it.key }.toSet()
        assertEquals(setOf("phở", "nước"), vocabKeys)
    }
}
