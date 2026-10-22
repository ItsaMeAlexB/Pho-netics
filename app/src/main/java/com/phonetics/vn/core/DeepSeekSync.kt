package com.phonetics.vn.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * The only thing that ever leaves the device: compact, anonymous per-(group,subgroup)
 * fractions plus question counts, weakest groups and flagged-question notes. Never raw
 * answer text, never voice, never PII (see ARCHITECTURE.md §7).
 */
object SyncPayload {
    fun build(state: LearnerState): String {
        val root = JSONObject()
        val groups = JSONArray()
        val byComponent = state.questions.values.groupBy { it.component }
        for ((comp, qs) in byComponent) {
            val atoms = qs.flatMap { it.atoms }.distinct()
            var attempts = 0
            var correct = 0
            for (a in atoms) {
                val s = state.stats[a]
                if (s != null) { attempts += s.attempts; correct += s.correct }
            }
            groups.put(JSONObject()
                .put("group", comp.group.display)
                .put("subgroup", comp.subgroup)
                .put("fraction", "$correct/$attempts")
                .put("questions", qs.size))
        }
        root.put("groups", groups)
        val flags = JSONArray()
        for ((id, note) in state.flags) flags.put(JSONObject().put("question", id).put("note", note))
        root.put("flags", flags)
        // Weakest = lowest success rate among non-empty components.
        val ranked = byComponent.entries
            .map { (c, qs) ->
                val atoms = qs.flatMap { it.atoms }.distinct()
                val att = atoms.sumOf { state.stats[it]?.attempts ?: 0 }
                val cor = atoms.sumOf { state.stats[it]?.correct ?: 0 }
                Triple(c, att, cor)
            }.filter { it.second > 0 }
            .sortedBy { if (it.third == 0) -1.0 else it.third.toDouble() / it.second }
            .take(3)
        val weakest = JSONArray()
        for ((c, att, cor) in ranked) weakest.put(JSONObject()
            .put("group", c.group.display)
            .put("subgroup", c.subgroup)
            .put("fraction", "$cor/$att"))
        root.put("weakest", weakest)
        return root.toString()
    }
}

/** A structured instruction the LLM may send back; applied locally, labels are advisory. */
sealed class SyncAction {
    data class Prioritize(val group: String, val subgroup: String) : SyncAction()
    data class Demote(val group: String, val subgroup: String) : SyncAction()
    data class Create(val question: Question) : SyncAction()
    data class Update(val id: String, val tier: Int?, val format: Format?) : SyncAction()
    data class Delete(val id: String) : SyncAction()
    data class Reclassify(val id: String, val group: KnowledgeGroup, val subgroup: String) : SyncAction()
    data class AddComponent(val group: KnowledgeGroup, val subgroup: String) : SyncAction()
    data class ReviewRequest(val group: String, val subgroup: String) : SyncAction()
}

object SyncActions {

    /** Parse the model's reply (optionally fenced in ```json) into typed actions. */
    fun parse(reply: String): List<SyncAction> {
        val text = reply.trim()
            .removePrefix("```json").removeSuffix("```")
            .removePrefix("```").removeSuffix("```")
            .trim()
        val root = JSONObject(text)
        val arr = root.optJSONArray("actions") ?: return emptyList()
        val out = mutableListOf<SyncAction>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            when (o.optString("type")) {
                "prioritize" -> out += SyncAction.Prioritize(o.optString("group"), o.optString("subgroup"))
                "demote" -> out += SyncAction.Demote(o.optString("group"), o.optString("subgroup"))
                "delete" -> out += SyncAction.Delete(o.optString("id"))
                "review_request" -> out += SyncAction.ReviewRequest(o.optString("group"), o.optString("subgroup"))
                "update" -> out += SyncAction.Update(
                    o.optString("id"),
                    if (o.has("tier")) o.optInt("tier") else null,
                    if (o.has("format")) Format.valueOf(o.optString("format")) else null,
                )
                "reclassify" -> out += SyncAction.Reclassify(
                    o.optString("id"),
                    KnowledgeGroup.valueOf(o.optString("group").uppercase()),
                    o.optString("subgroup"),
                )
                "add_component" -> out += SyncAction.AddComponent(
                    KnowledgeGroup.valueOf(o.optString("group").uppercase()),
                    o.optString("subgroup"),
                )
                "create" -> {
                    val qo = o.optJSONObject("question") ?: continue
                    val comp = Component(
                        KnowledgeGroup.valueOf(qo.optString("group").uppercase()),
                        qo.optString("subgroup"),
                    )
                    val variants = mutableListOf<String>()
                    val av = qo.optJSONArray("answer_variants")
                    if (av != null) for (i in 0 until av.length()) variants += av.getString(i)
                    // Atoms are derived locally; the LLM's atom tags are never trusted.
                    val q = Question(
                        id = qo.optString("id"),
                        component = comp,
                        format = Format.valueOf(qo.optString("format")),
                        tier = qo.optInt("tier"),
                        prompt = qo.optString("prompt"),
                        answers = listOf(qo.optString("answer")),
                        hasAudio = qo.optBoolean("hasAudio"),
                        needsMic = qo.optBoolean("needsMic"),
                        atoms = AtomParser.atomsFor(qo.optString("answer"), comp),
                        priority = false,
                        enabled = true,
                        acceptedAnswers = variants,
                        emoji = qo.optString("emoji"),
                    )
                    out += SyncAction.Create(q)
                }
            }
        }
        return out
    }

    /** Apply typed actions to local state. Returns human-readable log lines. */
    fun execute(actions: List<SyncAction>, state: LearnerState): List<String> {
        val log = mutableListOf<String>()
        outer@ for (a in actions) when (a) {
            is SyncAction.Prioritize -> {
                for (q in state.questions.values) {
                    if (q.component.group.display == a.group && q.component.subgroup == a.subgroup)
                        state.questions[q.id] = q.copy(priority = true)
                }
                log += "prioritized ${a.group}/${a.subgroup}"
            }
            is SyncAction.Demote -> {
                for (q in state.questions.values) {
                    if (q.component.group.display == a.group && q.component.subgroup == a.subgroup)
                        state.questions[q.id] = q.copy(priority = false)
                }
                log += "demoted ${a.group}/${a.subgroup}"
            }
            is SyncAction.Create -> {
                state.questions[a.question.id] = a.question
                log += "created question ${a.question.id}"
            }
            is SyncAction.Update -> {
                val q = state.questions[a.id] ?: continue@outer
                state.questions[a.id] = q.copy(tier = a.tier ?: q.tier, format = a.format ?: q.format)
                log += "updated ${a.id}"
            }
            is SyncAction.Delete -> {
                state.questions.remove(a.id)
                state.flags.remove(a.id)
                log += "deleted ${a.id}"
            }
            is SyncAction.Reclassify -> {
                val q = state.questions[a.id] ?: continue@outer
                state.questions[a.id] = q.copy(component = Component(a.group, a.subgroup))
                log += "reclassified ${a.id} -> ${a.group}/${a.subgroup}"
            }
            is SyncAction.AddComponent -> log += "added component ${a.group}/${a.subgroup}"
            is SyncAction.ReviewRequest -> log += "review requested for ${a.group}/${a.subgroup}"
        }
        return log
    }
}

/** HTTP seam so the sync client is testable without a network. */
fun interface SyncHttp {
    fun post(url: String, headers: Map<String, String>, body: String): String
}

/** DeepSeek (OpenAI-compatible, chat-only) sync client. Text-only, anonymous. */
class DeepSeekSync(private val http: SyncHttp) {
    fun run(state: LearnerState, apiKey: String, baseUrl: String, model: String): List<String> {
        require(apiKey.isNotBlank()) { "API key is required to sync" }
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val userContent = "Analyze this anonymous learning snapshot and reply ONLY with a JSON object with an \"actions\" array. " +
            "Each action is one of: prioritize/demote/add_component {group,subgroup}, create {question:{id,group,subgroup,format,tier,prompt,answer,emoji:simple emoji visual,answer_variants:[accepted paraphrase answers]}}, " +
            "update {id,tier,format}, delete {id}, reclassify {id,group,subgroup}, review_request {group,subgroup}. " +
            "Snapshot: " + SyncPayload.build(state)
        val userMsg = JSONObject().put("role", "user").put("content", userContent)
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray().put(userMsg))
            .toString()
        val resp = http.post(
            url,
            mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json"),
            body,
        )
        val content = JSONObject(resp)
            .optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?: throw IllegalStateException("No assistant content in sync response")
        return SyncActions.execute(SyncActions.parse(content), state)
    }
}

