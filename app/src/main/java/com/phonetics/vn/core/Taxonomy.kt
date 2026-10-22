package com.phonetics.vn.core

/** Main categories of what an English speaker must acquire, per the design doc (Part I). */
enum class KnowledgeGroup(val display: String) {
    PRONUNCIATION("Pronunciation"),
    VOCABULARY("Vocabulary"),
    GRAMMAR("Grammar"),
    PRAGMATICS("Pragmatics"),
}

/** A named area: (group, subgroup), e.g. (VOCABULARY, "numbers"). */
data class Component(val group: KnowledgeGroup, val subgroup: String)

/** Default taxonomy seeded from the doc's contrastive analysis; LLM may add more. */
fun seedComponents(): List<Component> = listOf(
    Component(KnowledgeGroup.PRONUNCIATION, "tones"),
    Component(KnowledgeGroup.PRONUNCIATION, "final-consonants"),
    Component(KnowledgeGroup.PRONUNCIATION, "vowels"),
    Component(KnowledgeGroup.VOCABULARY, "numbers"),
    Component(KnowledgeGroup.VOCABULARY, "greetings"),
    Component(KnowledgeGroup.VOCABULARY, "food-drink"),
    Component(KnowledgeGroup.VOCABULARY, "family"),
    Component(KnowledgeGroup.GRAMMAR, "word-order"),
    Component(KnowledgeGroup.GRAMMAR, "classifiers"),
    Component(KnowledgeGroup.GRAMMAR, "tense-particles"),
    Component(KnowledgeGroup.GRAMMAR, "questions"),
    Component(KnowledgeGroup.PRAGMATICS, "kinship-pronouns"),
    Component(KnowledgeGroup.PRAGMATICS, "politeness-particles"),
)
