package com.phonetics.vn.core

/** Presentation format of a question; higher tiers = harder, more productive. */
enum class Format {
    FLASHCARD, // recognition / MCQ
    MATCH,     // two-column mapping: Vietnamese words <-> English meanings
    TYPE,      // typed recall
    CLOZE,     // complete-the-sentence
    PARAGRAPH, // short Vietnamese passage + English comprehension follow-up (i+1 / elaborative interrogation)
    AUDIO,     // listening (play button)
    ORAL,      // speaking (mic button)
}

/**
 * A question lives in the bank with a component, a format tier, and the knowledge
 * atoms it exercises (derived locally at ingestion, never trusted from the LLM).
 */
data class Question(
    val id: String,
    val component: Component,
    val format: Format,
    val tier: Int,
    val prompt: String,
    val answers: List<String>,
    val hasAudio: Boolean = false,
    val needsMic: Boolean = false,
    val atoms: Set<Atom> = emptySet(),
    val priority: Boolean = false,
    val enabled: Boolean = true,
    /** For [Format.MATCH]: the (Vietnamese, English) pairs to map. */
    val matchPairs: List<Pair<String, String>> = emptyList(),
    /** Alternate acceptable answers/paraphrases (e.g. supplied by the LLM at creation time). */
    val acceptedAnswers: List<String> = emptyList(),
    /** Simple dual-coding visual cue (emoji) shown on the card. */
    val emoji: String = "",
) {
    val targetText: String get() = answers.firstOrNull() ?: ""
}
