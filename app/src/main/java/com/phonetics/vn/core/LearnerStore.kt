package com.phonetics.vn.core

import org.json.JSONArray
import org.json.JSONObject

/** Minimal persistence seam so the store is testable on the JVM without Android. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

/** All mutable learner data that must survive a relaunch. */
class LearnerState {
    val questions = linkedMapOf<String, Question>() // id -> working copy (priority/enabled mutate here)
    val stats = mutableMapOf<Atom, AtomStat>()
    val flags = mutableMapOf<String, String>() // question id -> learner note
    val settings = sortedMapOf<String, String>()

    fun statFor(a: Atom): AtomStat = stats.getOrPut(a) { AtomStat(a) }

    fun reset() {
        questions.clear()
        stats.clear()
        flags.clear()
        settings.clear()
    }

    fun seedFromBank(bank: List<Question> = SeedBank.questions()) {
        reset()
        for (q in bank) questions[q.id] = q
    }
}


/** Pure org.json encode/decode of [LearnerState] (JVM-testable via org.json:json). */
object StoreCodec {

    private fun atomToJson(a: Atom) = JSONObject()
        .put("kind", a.kind.name)
        .put("key", a.key)
        .put("group", a.component.group.name)
        .put("subgroup", a.component.subgroup)

    private fun atomFromJson(o: JSONObject) = Atom(
        AtomKind.valueOf(o.optString("kind")),
        o.optString("key"),
        Component(KnowledgeGroup.valueOf(o.optString("group")), o.optString("subgroup")),
    )

    private fun questionToJson(q: Question): JSONObject {
        val atoms = JSONArray()
        for (a in q.atoms) atoms.put(atomToJson(a))
        val mp = JSONArray()
        for ((vn, en) in q.matchPairs) mp.put(JSONObject().put("vn", vn).put("en", en))
        return JSONObject()
            .put("id", q.id)
            .put("group", q.component.group.name)
            .put("subgroup", q.component.subgroup)
            .put("format", q.format.name)
            .put("tier", q.tier)
            .put("prompt", q.prompt)
            .put("answers", JSONArray(q.answers))
            .put("hasAudio", q.hasAudio)
            .put("needsMic", q.needsMic)
            .put("atoms", atoms)
            .put("priority", q.priority)
            .put("enabled", q.enabled)
            .put("matchPairs", mp)
            .put("acceptedAnswers", JSONArray(q.acceptedAnswers))
            .put("emoji", q.emoji)
    }

    private fun questionFromJson(o: JSONObject): Question {
        val atoms = mutableSetOf<Atom>()
        val aa = o.optJSONArray("atoms")
        if (aa != null) for (i in 0 until aa.length()) atoms += atomFromJson(aa.getJSONObject(i))
        val answers = mutableListOf<String>()
        val an = o.optJSONArray("answers")
        if (an != null) for (i in 0 until an.length()) answers += an.getString(i)
        val pairs = mutableListOf<Pair<String, String>>()
        val mp = o.optJSONArray("matchPairs")
        if (mp != null) for (i in 0 until mp.length()) {
            val po = mp.getJSONObject(i)
            pairs += po.optString("vn") to po.optString("en")
        }
        val accepted = mutableListOf<String>()
        val acc = o.optJSONArray("acceptedAnswers")
        if (acc != null) for (i in 0 until acc.length()) accepted += acc.getString(i)
        return Question(
            id = o.optString("id"),
            component = Component(KnowledgeGroup.valueOf(o.optString("group")), o.optString("subgroup")),
            format = Format.valueOf(o.optString("format")),
            tier = o.optInt("tier"),
            prompt = o.optString("prompt"),
            answers = answers,
            hasAudio = o.optBoolean("hasAudio"),
            needsMic = o.optBoolean("needsMic"),
            atoms = atoms,
            priority = o.optBoolean("priority"),
            enabled = o.optBoolean("enabled", true),
            matchPairs = pairs,
            acceptedAnswers = accepted,
            emoji = o.optString("emoji"),
        )
    }

    private fun statToJson(a: Atom, s: AtomStat): JSONObject = atomToJson(a)
        .put("attempts", s.attempts)
        .put("correct", s.correct)
        .put("stability", s.stability)
        .put("difficulty", s.difficulty)
        .put("lastReviewMs", s.lastReviewMs)

    private fun statFromJson(o: JSONObject): Pair<Atom, AtomStat> {
        val a = atomFromJson(o)
        return a to AtomStat(
            atom = a,
            attempts = o.optInt("attempts"),
            correct = o.optInt("correct"),
            stability = o.optDouble("stability", 2.0),
            difficulty = o.optDouble("difficulty", 5.0),
            lastReviewMs = o.optLong("lastReviewMs"),
        )
    }

    fun toJson(state: LearnerState): String {
        val root = JSONObject()
        val qa = JSONArray()
        for (q in state.questions.values) qa.put(questionToJson(q))
        root.put("questions", qa)
        val sa = JSONArray()
        for ((a, s) in state.stats) sa.put(statToJson(a, s))
        root.put("stats", sa)
        root.put("flags", JSONObject(state.flags))
        root.put("settings", JSONObject(state.settings))
        return root.toString()
    }

    fun fromJson(raw: String): LearnerState {
        val st = LearnerState()
        if (raw.isBlank()) { st.seedFromBank(); return st }
        val root = JSONObject(raw)
        val qa = root.optJSONArray("questions")
        if (qa != null) for (i in 0 until qa.length()) {
            val q = questionFromJson(qa.getJSONObject(i))
            st.questions[q.id] = q
        }
        val sa = root.optJSONArray("stats")
        if (sa != null) for (i in 0 until sa.length()) {
            val (a, s) = statFromJson(sa.getJSONObject(i))
            st.stats[a] = s
        }
        val fl = root.optJSONObject("flags")
        if (fl != null) for (k in fl.keys()) st.flags[k] = fl.optString(k)
        val se = root.optJSONObject("settings") ?: JSONObject()
        for (k in se.keys()) st.settings[k] = se.optString(k)
        if (st.settings["base_url"].isNullOrEmpty()) st.settings["base_url"] = "https://api.deepseek.com"
        if (st.settings["model"].isNullOrEmpty()) st.settings["model"] = "deepseek-chat"
        return st
    }
}


/**
 * Loads/saves [LearnerState] through a [KeyValueStore]. First run seeds the bank from
 * [SeedBank]; the learning engine then mutates and persists it.
 */
class LearnerStore(private val kv: KeyValueStore) {

    private val key = "learner_state_v1"

    fun load(): LearnerState {
        val raw = kv.get(key)
        return if (raw.isNullOrBlank()) {
            val st = LearnerState().apply { seedFromBank() }
            st.settings["base_url"] = "https://api.deepseek.com"
            st.settings["model"] = "deepseek-chat"
            save(st)
            st
        } else StoreCodec.fromJson(raw)
    }

    fun save(state: LearnerState) {
        kv.put(key, StoreCodec.toJson(state))
    }
}
