package com.phonetics.vn.core

/**
 * Small hand-authored starter bank (~20 questions across the taxonomy). Atoms are
 * derived locally via [AtomParser]; the LLM grows the bank after the first sync.
 */
object SeedBank {

    private class S(
        val id: String,
        val g: KnowledgeGroup,
        val sub: String,
        val f: Format,
        val prompt: String,
        val answers: List<String> = emptyList(),
        val audio: Boolean = false,
        val mic: Boolean = false,
        val tier: Int = 0,
        val pairs: List<Pair<String, String>> = emptyList(),
        val accepted: List<String> = emptyList(),
        val emoji: String = "",
    ) {
        fun q(): Question {
            val comp = Component(g, sub)
            val atoms = when {
                f == Format.MATCH && pairs.isNotEmpty() ->
                    AtomParser.atomsFor(pairs.joinToString(" ") { it.first }, comp)
                answers.isNotEmpty() -> AtomParser.atomsFor(answers.first(), comp)
                else -> emptySet()
            }
            val ans = if (f == Format.MATCH && pairs.isNotEmpty()) pairs.map { it.first } else answers
            return Question(
                id = id, component = comp, format = f, tier = tier, prompt = prompt,
                answers = ans, hasAudio = audio, needsMic = mic, atoms = atoms,
                matchPairs = pairs, acceptedAnswers = accepted, emoji = emoji,
            )
        }
    }

    fun questions(): List<Question> = listOf(
        S("s01", KnowledgeGroup.VOCABULARY, "numbers", Format.FLASHCARD, "How do you say \"five\"?", listOf("năm"), emoji = "🖐️"),
        S("s02", KnowledgeGroup.VOCABULARY, "numbers", Format.TYPE, "Type \"three\" in Vietnamese.", listOf("ba"), emoji = "3️⃣"),
        S("s03", KnowledgeGroup.VOCABULARY, "greetings", Format.FLASHCARD, "What is \"hello\"?", listOf("xin chào"), emoji = "👋"),
        S("s04", KnowledgeGroup.VOCABULARY, "greetings", Format.TYPE, "Type \"thank you\".", listOf("cảm ơn"), emoji = "🙏"),
        S("s05", KnowledgeGroup.VOCABULARY, "food-drink", Format.MATCH, "Match each food/drink to its meaning.",
            pairs = listOf("phở" to "noodle soup", "nước" to "water", "cà phê" to "coffee")),
        S("s06", KnowledgeGroup.VOCABULARY, "food-drink", Format.TYPE, "Type \"water\".", listOf("nước"), emoji = "💧"),
        S("s07", KnowledgeGroup.VOCABULARY, "family", Format.FLASHCARD, "What is \"mother\"?", listOf("mẹ"), emoji = "👩"),
        S("s08", KnowledgeGroup.VOCABULARY, "family", Format.TYPE, "Type \"older brother\".", listOf("anh trai"), emoji = "👦"),
        S("s23", KnowledgeGroup.VOCABULARY, "numbers", Format.MATCH, "Match each number to its English.",
            pairs = listOf("một" to "one", "hai" to "two", "ba" to "three")),
        S("s24", KnowledgeGroup.VOCABULARY, "family", Format.MATCH, "Match each family member to its meaning.",
            pairs = listOf("mẹ" to "mother", "bố" to "father", "anh trai" to "older brother")),
        S("s25", KnowledgeGroup.VOCABULARY, "food-drink", Format.PARAGRAPH,
            "Nam thích phở và cà phê.\nNam likes ... How many things does Nam like?", listOf("two"),
            accepted = listOf("2", "two things", "2 things")),
        S("s26", KnowledgeGroup.PRAGMATICS, "kinship-pronouns", Format.PARAGRAPH,
            "An nói: \"Chào cô.\" Cô là chị của bố An.\nWho is An greeting?", listOf("aunt"),
            accepted = listOf("an aunt", "my aunt", "his aunt", "the aunt")),
        S("s27", KnowledgeGroup.GRAMMAR, "word-order", Format.PARAGRAPH,
            "Con chó to màu đỏ.\nThe big dog is what colour?", listOf("red"),
            accepted = listOf("it's red", "red color", "red colour", "the colour red", "the color red")),
        S("s28", KnowledgeGroup.GRAMMAR, "questions", Format.PARAGRAPH,
            "Bạn đi đâu? Tôi đi chợ.\nWhere is the speaker going?", listOf("market"),
            accepted = listOf("to the market", "the market", "to the shops", "shopping")),
        S("s09", KnowledgeGroup.PRONUNCIATION, "tones", Format.AUDIO, "Which tone did you hear?", listOf("má"), audio = true),
        S("s10", KnowledgeGroup.PRONUNCIATION, "tones", Format.AUDIO, "Which tone did you hear?", listOf("mạ"), audio = true),
        S("s11", KnowledgeGroup.PRONUNCIATION, "final-consonants", Format.AUDIO, "Which final consonant did you hear?", listOf("mát"), audio = true),
        S("s12", KnowledgeGroup.PRONUNCIATION, "vowels", Format.AUDIO, "Which vowel did you hear?", listOf("êm"), audio = true),
        S("s13", KnowledgeGroup.GRAMMAR, "word-order", Format.CLOZE, "Make \"the big dog\": _ _ _", listOf("con chó to"), emoji = "🐕"),
        S("s14", KnowledgeGroup.GRAMMAR, "word-order", Format.TYPE, "Type \"the red car\".", listOf("xe đỏ"), emoji = "🚗"),
        S("s15", KnowledgeGroup.GRAMMAR, "classifiers", Format.CLOZE, "Fill in the classifier: hai _ chó", listOf("hai con chó"), emoji = "🐶"),
        S("s16", KnowledgeGroup.GRAMMAR, "classifiers", Format.TYPE, "Type \"a book\" (with classifier).", listOf("quyển sách"), emoji = "📕"),
        S("s17", KnowledgeGroup.GRAMMAR, "tense-particles", Format.CLOZE, "I ate: tôi _ ăn", listOf("tôi đã ăn"), emoji = "🍽️"),
        S("s18", KnowledgeGroup.GRAMMAR, "questions", Format.TYPE, "Where are you going? (Bạn đi _)", listOf("bạn đi đâu")),
        S("s19", KnowledgeGroup.PRAGMATICS, "politeness-particles", Format.CLOZE, "Polite thanks: cảm ơn _", listOf("cảm ơn ạ")),
        S("s20", KnowledgeGroup.PRAGMATICS, "kinship-pronouns", Format.TYPE, "Greet an older woman (aunt): chào _", listOf("chào cô")),
        S("s21", KnowledgeGroup.PRAGMATICS, "politeness-particles", Format.FLASHCARD, "Polite \"yes\": ...", listOf("dạ vâng")),
        S("s22", KnowledgeGroup.PRAGMATICS, "kinship-pronouns", Format.ORAL, "Say a polite greeting to a peer.", listOf("chào bạn"), mic = true),
    ).map { it.q() }
}
