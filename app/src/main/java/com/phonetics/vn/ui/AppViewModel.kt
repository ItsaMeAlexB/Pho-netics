package com.phonetics.vn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phonetics.vn.core.AnswerDiffer
import com.phonetics.vn.core.AtomUpdater
import com.phonetics.vn.core.DeepSeekSync
import com.phonetics.vn.core.LearnerState
import com.phonetics.vn.core.LearnerStore
import com.phonetics.vn.core.Question
import com.phonetics.vn.core.QuestionEngine
import com.phonetics.vn.core.Scheduler
import com.phonetics.vn.core.SubmissionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val current: Question? = null,
    val options: List<String> = emptyList(),
    val answered: Boolean = false,
    val showAnswer: Boolean = false,
    val typed: String = "",
    val selected: String? = null,
    val feedback: SubmissionResult? = null,
    val skipped: Boolean = false,
    val micListening: Boolean = false,
    val progressLine: String = "",
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LearnerStore(PrefsKeyValueStore(application))
    val state: LearnerState = store.load()
    private val engine = QuestionEngine(state, Scheduler(), AtomUpdater(), AnswerDiffer())
    private val sync = DeepSeekSync(HttpSync())
    private val player = SpeechPlayer(application)
    private val mic = MicRecorder(application) { text -> onSpeechResult(text) }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val _progress = MutableStateFlow(emptyList<ComponentScore>())
    val progress: StateFlow<List<ComponentScore>> = _progress

    private val _flags = MutableStateFlow<Map<String, String>>(emptyMap())
    val flags: StateFlow<Map<String, String>> = _flags

    private val _syncLog = MutableStateFlow(emptyList<String>())
    val syncLog: StateFlow<List<String>> = _syncLog

    private val _screen = MutableStateFlow(AppScreen.STACK)
    val screen: StateFlow<AppScreen> = _screen

    init {
        refresh()
        advance()
    }

    fun show(s: AppScreen) { _screen.value = s }

    private fun persist() = store.save(state)

    private fun refresh() {
        val byComp = state.questions.values.groupBy { it.component }
        _progress.value = byComp.map { (c, qs) ->
            val atoms = qs.flatMap { it.atoms }.distinct()
            ComponentScore(c, atoms.sumOf { state.stats[it]?.attempts ?: 0 }, atoms.sumOf { state.stats[it]?.correct ?: 0 })
        }.sortedBy { it.component.group.name + it.component.subgroup }
        _flags.value = state.flags.toMap()
        val learned = state.questions.values.count { q -> q.atoms.all { (state.stats[it]?.attempts ?: 0) > 0 } }
        _ui.value = _ui.value.copy(progressLine =
            "Learned $learned/${state.questions.size} · enabled ${state.questions.values.count { it.enabled }}")
    }

    fun advance() {
        val q = engine.next()
        _ui.value = UiState(current = q, progressLine = _ui.value.progressLine)
        if (q != null) refresh()
    }

    fun onTyped(v: String) { _ui.value = _ui.value.copy(typed = v) }

    /** Submit a typed/graded answer for the current question. */
    fun submit(given: String, fromSpeech: Boolean = false) {
        val q = _ui.value.current ?: return
        val res = engine.submit(q, given, fromSpeech)
        persist()
        _ui.value = _ui.value.copy(answered = true, feedback = res)
        refresh()
    }

    /** Submit a two-column [Format.MATCH] answer from the linked pairs. */
    fun submitMatch(pairs: List<Pair<String, String>>) {
        val q = _ui.value.current ?: return
        val res = engine.submitMatch(q, pairs)
        persist()
        _ui.value = _ui.value.copy(answered = true, feedback = res)
        refresh()
    }

    /** Recognition formats: learner knew it -> scored correct (advance follows). */
    fun knownIt() {
        val q = _ui.value.current ?: return
        submit(q.targetText, fromSpeech = false)
    }

    /** Skip: reveal the answer and score this first exposure as wrong; Continue advances.
     *  The wrong attempt is recorded once and never negated by a later correct attempt. */
    fun skip() {
        val q = _ui.value.current ?: return
        val res = engine.submit(q, "") // wrong first attempt
        persist()
        _ui.value = _ui.value.copy(answered = true, showAnswer = true, skipped = true, feedback = res)
        refresh()
    }

    /** Retry the same card after a wrong attempt (the wrong attempt stays recorded). */
    fun tryAgain() {
        _ui.value = _ui.value.copy(
            answered = false, showAnswer = false, skipped = false,
            feedback = null, typed = "", selected = null,
        )
    }

    fun flagCurrent(note: String) {
        val q = _ui.value.current ?: return
        state.flags[q.id] = note
        persist()
        refresh()
    }

    fun togglePriority(id: String) {
        val q = state.questions[id] ?: return
        state.questions[id] = q.copy(priority = !q.priority)
        persist()
        refresh()
    }

    fun setSetting(key: String, value: String) { state.settings[key] = value; persist() }
    fun apiKey(): String = state.settings["api_key"] ?: ""

    /** Play the current question's target aloud (on-device TTS) when a voice is available. */
    fun playCurrent() { player.speak(_ui.value.current?.targetText ?: "") }
    fun speechAvailable(): Boolean = player.available()

    /** Start recording a spoken answer (ORAL). */
    fun startMic() {
        try {
            mic.start()
            _ui.value = _ui.value.copy(micListening = true)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(micListening = false)
        }
    }

    fun stopMic() { mic.stop() }

    private fun onSpeechResult(text: String) {
        _ui.value = _ui.value.copy(micListening = false)
        // A blank transcript (or recognizer error) just ends listening; let the learner retry.
        if (text.isBlank()) return
        submit(text, fromSpeech = true)
    }

    override fun onCleared() {
        mic.destroy()
        player.shutdown()
        super.onCleared()
    }

    /** Optional DeepSeek sync, run off the UI thread. */
    fun syncNow() {
        val key = apiKey()
        if (key.isBlank()) {
            _syncLog.value = listOf("Set your DeepSeek API key in Settings before syncing.")
            return
        }
        viewModelScope.launch {
            try {
                val log = sync.run(
                    state,
                    key,
                    state.settings["base_url"] ?: "https://api.deepseek.com",
                    state.settings["model"] ?: "deepseek-chat",
                )
                persist()
                refresh()
                _syncLog.value = log
            } catch (e: Exception) {
                _syncLog.value = listOf("Sync failed: ${e.message}")
            }
        }
    }
}
