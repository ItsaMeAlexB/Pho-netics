package com.phonetics.vn.core

/** Result of one answered question. */
data class SubmissionResult(val question: Question, val diagnosis: Diagnosis)

/**
 * Orchestrates the learning loop on a [LearnerState]: pick the next question, grade the
 * learner's answer via [AnswerDiffer], and apply the [AtomUpdater] to the implicated atoms
 * (falling back to equal-share blame when the diff can't isolate a bad atom). Pure and
 * Android-free so it's unit-testable; persistence is the caller's concern.
 */
class QuestionEngine(
    val state: LearnerState,
    private val scheduler: Scheduler = Scheduler(),
    private val updater: AtomUpdater = AtomUpdater(),
    private val differ: AnswerDiffer = AnswerDiffer(),
) {

    /** The next question to show, or null if none remain enabled. */
    fun next(): Question? = scheduler.next(state.questions.values.toList(), state.stats)

    /**
     * Grades [given] against the question's target. On success every atom is rewarded; on
     * failure only the atoms the diff implicates are punished (or all atoms equally if no
     * atom can be isolated). Returns the diagnosis for the UI to show feedback.
     */
    fun submit(q: Question, given: String, fromSpeech: Boolean = false): SubmissionResult =
        grade(q, differ.analyzeAny(listOf(q.targetText) + q.acceptedAnswers, given, fromSpeech))

    /**
     * Grades a [Format.MATCH] question from the learner's linking — exact pair matching,
     * not free-text. Each Vietnamese word that wasn't mapped to its English meaning is a
     * VOCAB failure.
     */
    fun submitMatch(q: Question, given: List<Pair<String, String>>): SubmissionResult {
        val expected = q.matchPairs.toSet()
        val provided = given.toSet()
        val allCorrect = expected.isNotEmpty() && provided == expected
        val failures = if (allCorrect) emptyList()
        else expected.filter { it !in provided }.map { DiagnosedFailure(AtomKind.VOCAB, it.first) }
        return grade(q, Diagnosis(allCorrect, failures))
    }

    private fun grade(q: Question, diag: Diagnosis): SubmissionResult {
        if (diag.correct) {
            for (a in q.atoms) updater.onCorrect(state.statFor(a))
        } else {
            val implicated = implicatedAtoms(q, diag.failures)
            if (implicated.isNotEmpty()) {
                for (st in implicated) updater.onWrong(st)
            } else {
                updater.onAmbiguousWrong(q.atoms.map { state.statFor(it) })
            }
        }
        return SubmissionResult(q, diag)
    }

    private fun implicatedAtoms(q: Question, failures: List<DiagnosedFailure>): List<AtomStat> =
        q.atoms.filter { a -> failures.any { f -> implies(f, a) } }.map { state.statFor(it) }

    /** A failure implicates an atom when kinds match and the keys line up (TONE atoms are
     *  stored under "tone:<token>" while the diff reports the bare token). */
    private fun implies(f: DiagnosedFailure, a: Atom): Boolean {
        if (f.kind != a.kind) return false
        if (a.key == f.key) return true
        return f.kind == AtomKind.TONE && a.key == "tone:${f.key}"
    }
}
