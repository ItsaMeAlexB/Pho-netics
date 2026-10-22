package com.phonetics.vn.core

import com.phonetics.vn.core.AnswerDiffer.Companion.isFunctionWord
import com.phonetics.vn.core.AnswerDiffer.Companion.isPolitenessMarker
import com.phonetics.vn.core.AnswerDiffer.Companion.tokenize

/**
 * Derives knowledge atoms locally from a question's target answer text, so scores are
 * grounded in local parsing and never trusted from the LLM. Because Vietnamese
 * orthography marks tone with diacritics, a token carrying diacritics also contributes
 * a TONE atom; function/classifier words contribute GRAMMAR; politeness markers contribute
 * PRAGMATICS; everything else contributes VOCAB.
 */
object AtomParser {

    fun atomsFor(targetText: String, component: Component): Set<Atom> {
        val out = mutableSetOf<Atom>()
        for (token in tokenize(targetText)) {
            when {
                isPolitenessMarker(token) -> out += Atom(AtomKind.PRAGMATICS, token, component)
                isFunctionWord(token) -> {
                    out += Atom(AtomKind.GRAMMAR, token, component)
                    if (hasDiacritics(token)) out += Atom(AtomKind.TONE, "tone:$token", component)
                }
                else -> {
                    out += Atom(AtomKind.VOCAB, token, component)
                    if (hasDiacritics(token)) out += Atom(AtomKind.TONE, "tone:$token", component)
                }
            }
        }
        return out
    }

    /** Vietnamese accented/tone-marked characters are all above ASCII. */
    private fun hasDiacritics(s: String): Boolean = s.any { it.code > 127 }
}
