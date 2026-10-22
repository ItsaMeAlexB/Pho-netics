package com.phonetics.vn.core

import java.text.Normalizer

/** One localized failure produced by the diff engine. */
data class DiagnosedFailure(val kind: AtomKind, val key: String)

/** Overall verdict + the refined atom blame for a single answer. */
data class Diagnosis(val correct: Boolean, val failures: List<DiagnosedFailure>) {
    val isAmbiguous: Boolean get() = !correct && failures.isEmpty()
}

/**
 * The answer-diff engine. Compares the learner's answer (typed OR ASR transcript) to
 * the target. Because Vietnamese orthography writes base letters = sounds and
 * diacritics = tone/vowel quality, simple string ops separate errors into the right
 * atoms with no extra ML:
 *   - equal base letters, different diacritics  -> TONE
 *   - near by edit distance (typed)             -> SPELLING
 *   - near by edit distance (spoken)            -> SOUND
 *   - whole-word substitution / omission        -> VOCAB (or GRAMMAR/PRAGMATICS for markers)
 *   - token-count mismatch / order              -> SYNTAX
 */
class AnswerDiffer(private val maxEditDistance: Int = 2) {

    fun analyze(target: String, given: String, fromSpeech: Boolean): Diagnosis {
        if (target.trim() == given.trim()) return Diagnosis(true, emptyList())
        return Diagnosis(false, diagnose(target, given, fromSpeech))
    }

    /**
     * Grades against several accepted answers (paraphrases). Correct if [given] matches any
     * of them (exact or near); otherwise reports the diff against the primary target. This is
     * how we accept "Tôi tên là John" alongside "Tên tôi là John" without a cloud round-trip —
     * the acceptable variants are supplied locally (or by the LLM at question-creation time).
     */
    fun analyzeAny(targets: List<String>, given: String, fromSpeech: Boolean): Diagnosis =
        targets.firstOrNull { analyze(it, given, fromSpeech).correct }?.let { Diagnosis(true, emptyList()) }
            ?: analyze(targets.firstOrNull() ?: "", given, fromSpeech)

    private fun diagnose(target: String, given: String, fromSpeech: Boolean): List<DiagnosedFailure> {
        val t = tokenize(target)
        val g = tokenize(given)
        val out = mutableListOf<DiagnosedFailure>()
        if (t.isEmpty()) return out

        // Pure reordering (same words, different order) is a syntax error.
        if (t.size == g.size && t != g && t.sorted() == g.sorted()) {
            return listOf(DiagnosedFailure(AtomKind.SYNTAX, "word-order:$t"))
        }

        if (t.size == g.size) {
            for (i in t.indices) {
                val a = t[i]
                val b = g[i]
                if (a == b) continue
                if (stripDiacritics(a) == stripDiacritics(b)) {
                    out += DiagnosedFailure(AtomKind.TONE, a)
                } else if (editDistance(a, b) in 1..maxEditDistance) {
                    out += DiagnosedFailure(if (fromSpeech) AtomKind.SOUND else AtomKind.SPELLING, a)
                } else {
                    out += categorize(a)
                }
            }
        } else {
            val remaining = g.toMutableList()
            for (a in t) {
                val exact = remaining.indexOf(a)
                if (exact >= 0) { remaining.removeAt(exact); continue }
                val near = remaining.indexOfFirst { stripDiacritics(it) == stripDiacritics(a) }
                if (near >= 0) { out += DiagnosedFailure(AtomKind.TONE, a); remaining.removeAt(near); continue }
                out += categorize(a)
            }
            if (remaining.isNotEmpty()) out += DiagnosedFailure(AtomKind.SYNTAX, "word-order:$remaining")
        }
        return out
    }

    private fun categorize(token: String): DiagnosedFailure = when {
        isPolitenessMarker(token) -> DiagnosedFailure(AtomKind.PRAGMATICS, token)
        isFunctionWord(token) -> DiagnosedFailure(AtomKind.GRAMMAR, token)
        else -> DiagnosedFailure(AtomKind.VOCAB, token)
    }

    companion object {

        internal fun tokenize(s: String): List<String> =
            s.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        /** Vietnamese: NFD-decompose and drop combining marks, then lowercase. */
        internal fun stripDiacritics(s: String): String {
            val nfd = Normalizer.normalize(s, Normalizer.Form.NFD)
            val sb = StringBuilder()
            for (c in nfd) {
                if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) sb.append(c)
            }
            return sb.toString()
        }

        internal fun editDistance(a: String, b: String): Int {
            val n = a.length
            val m = b.length
            val dp = IntArray(m + 1) { it }
            for (i in 1..n) {
                var prev = dp[0]
                dp[0] = i
                for (j in 1..m) {
                    val tmp = dp[j]
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                    prev = tmp
                }
            }
            return dp[m]
        }

        internal fun isFunctionWord(w: String): Boolean = when (w) {
            "đã", "sẽ", "đang", "đừng", "không", "và", "với", "của", "ở", "là", "thì",
            "mà", "cho", "cũng", "cái", "con", "chiếc", "quyển", "tờ", "người", "cây" -> true
            else -> false
        }

        internal fun isPolitenessMarker(w: String): Boolean = when (w) {
            "ạ", "dạ", "nhé", "nha", "thưa", "ơi", "vâng" -> true
            else -> false
        }
    }
}
