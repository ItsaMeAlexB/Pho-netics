package com.phonetics.vn.core

/** The granular failure dimension an atom tracks, per the diff engine. */
enum class AtomKind {
    VOCAB,       // wrong word / wrong meaning
    SPELLING,    // correct word, wrong letters (typed answers only)
    SOUND,       // consonant/vowel substitution (spoken answers)
    TONE,        // wrong tonal diacritic
    SYNTAX,      // word order / missing / extra tokens
    GRAMMAR,     // function words, tense particles, classifiers, questions
    PRAGMATICS,  // politeness register, particles, kinship pronouns
}

/** A single trackable piece of knowledge (a word, a tone, an explicit rule). */
data class Atom(val kind: AtomKind, val key: String, val component: Component)
