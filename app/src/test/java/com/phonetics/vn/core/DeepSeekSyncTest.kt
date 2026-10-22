package com.phonetics.vn.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekSyncTest {

    private fun state(): LearnerState = LearnerState().apply { seedFromBank() }

    @Test
    fun payloadIsAnonymousFractionSnapshot() {
        val st = state()
        val q = st.questions["s01"]!! // "năm"
        val res = QuestionEngine(st).submit(q, "năm")
        assertTrue(res.diagnosis.correct)
        val json = JSONObject(SyncPayload.build(st))
        assertTrue(json.has("groups"))
        val groups = json.getJSONArray("groups")
        var found = false
        for (i in 0 until groups.length()) {
            val o = groups.getJSONObject(i)
            if (o.getString("subgroup") == "numbers") { found = true; break }
        }
        assertTrue(found)
    }

    @Test
    fun parseHandlesFencedJsonAndMultiActionReply() {
        val reply = """```json
        {"actions": [
          {"type":"prioritize","group":"Vocabulary","subgroup":"numbers"},
          {"type":"delete","id":"s09"},
          {"type":"update","id":"s03","tier":2},
          {"type":"reclassify","id":"s02","group":"Grammar","subgroup":"word-order"},
          {"type":"review_request","group":"Grammar","subgroup":"classifiers"}
        ]}
        ```"""
        val acts = SyncActions.parse(reply)
        assertEquals(5, acts.size)
        assertTrue(acts.any { it is SyncAction.Prioritize })
        assertTrue(acts.any { it is SyncAction.Delete && it.id == "s09" })
        assertTrue(acts.any { it is SyncAction.Update && it.tier == 2 })
        assertTrue(acts.any { it is SyncAction.Reclassify && it.group == KnowledgeGroup.GRAMMAR })
        assertTrue(acts.any { it is SyncAction.ReviewRequest })
    }

    @Test
    fun createDerivesAtomsLocallyIgnoringLlmTags() {
        val reply = """{"actions":[
          {"type":"create","question":{"id":"c1","group":"Vocabulary","subgroup":"food-drink",
            "format":"TYPE","tier":1,"prompt":"Type water","answer":"nước",
            "answer_variants":["nước uống","nuoc","water"],"emoji":"💧",
            "atoms":[{"kind":"GRAMMAR","key":"fake"}]}}
        ]}"""
        val st = state()
        val log = SyncActions.execute(SyncActions.parse(reply), st)
        assertTrue(log.any { it.startsWith("created") })
        val q = st.questions["c1"]!!
        assertEquals("nước", q.targetText)
        assertTrue(q.atoms.none { it.key == "fake" }) // LLM atom tag ignored
        assertTrue(q.atoms.any { it.kind == AtomKind.VOCAB && it.key == "nước" })
        // The LLM's accepted paraphrases are kept and graded locally.
        assertTrue(q.acceptedAnswers.contains("nước uống"))
        assertTrue(q.acceptedAnswers.contains("water"))
        assertTrue(q.emoji.isNotBlank()) // emoji cue is captured too
    }

    @Test
    fun runAppliesActionsFromHttpResponse() {
        val reply = """{"choices":[{"message":{"content":
          "{\"actions\":[{\"type\":\"prioritize\",\"group\":\"Vocabulary\",\"subgroup\":\"numbers\"}]}"
        }}]}"""
        val http = SyncHttp { _, _, _ -> reply }
        val st = state()
        val log = DeepSeekSync(http).run(st, "sk-x", "https://api.deepseek.com", "deepseek-chat")
        assertTrue(log.any { it.startsWith("prioritized") })
        assertTrue(st.questions["s01"]!!.priority)
    }
}
