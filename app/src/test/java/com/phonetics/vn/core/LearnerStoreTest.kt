package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryKV : KeyValueStore {
    val map = mutableMapOf<String, String>()
    override fun get(key: String): String? = map[key]
    override fun put(key: String, value: String) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

class LearnerStoreTest {

    @Test
    fun freshStoreSeedsTheBankAndPersists() {
        val kv = InMemoryKV()
        val st = LearnerStore(kv)
        val state = st.load()
        assertTrue(state.questions.size >= 20)
        assertTrue(kv.map.containsKey("learner_state_v1"))
        assertEquals("https://api.deepseek.com", state.settings["base_url"])
    }

    @Test
    fun saveThenLoadPreservesEverything() {
        val kv = InMemoryKV()
        val store = LearnerStore(kv)
        val state = store.load()

        // Record some learning.
        val q = state.questions["s02"]!!
        val eng = QuestionEngine(state)
        eng.submit(q, "ba") // correct
        eng.submit(q, "xin") // wrong -> vocab miss
        state.flags["s03"] = "need to hear this again"
        state.settings["api_key"] = "sk-test"
        state.questions["s01"] = state.questions["s01"]!!.copy(priority = true)
        store.save(state)

        val reloaded = LearnerStore(kv).load()
        assertEquals(state.questions.size, reloaded.questions.size)
        assertEquals("sk-test", reloaded.settings["api_key"])
        assertEquals("need to hear this again", reloaded.flags["s03"])
        assertTrue(reloaded.questions["s01"]!!.priority)

        val s2 = reloaded.questions["s02"]!!
        val a = s2.atoms.first { it.key == "ba" }
        assertTrue((reloaded.stats[a]?.attempts ?: 0) >= 1)

        // MATCH pairs survived re-encode.
        val m = reloaded.questions["s05"]!!
        assertEquals(Format.MATCH, m.format)
        assertEquals(3, m.matchPairs.size)
        assertEquals("noodle soup", m.matchPairs.first { it.first == "phở" }.second)

        // Accepted answer variants survived re-encode.
        val p = reloaded.questions["s28"]!!
        assertTrue(p.acceptedAnswers.contains("to the market"))
        assertTrue(p.acceptedAnswers.contains("shopping"))

        // Emoji dual-coding cue survived re-encode.
        assertTrue(reloaded.questions["s13"]!!.emoji.isNotBlank())

        // Stats key survived re-encode: scheduler can still see them.
        val sched = Scheduler()
        val next = sched.next(reloaded.questions.values.toList(), reloaded.stats)
        assertTrue(next != null)
    }
}
