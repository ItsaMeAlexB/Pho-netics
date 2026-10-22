package com.phonetics.vn.ui

import android.content.Context
import com.phonetics.vn.core.Component
import com.phonetics.vn.core.KeyValueStore
import com.phonetics.vn.core.SyncHttp
import java.net.HttpURLConnection
import java.net.URL

/** SharedPreferences-backed [KeyValueStore]. */
class PrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("phonetics", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

/** HttpURLConnection-backed [SyncHttp] (Android built-in, no extra dependency). */
class HttpSync : SyncHttp {
    override fun post(url: String, headers: Map<String, String>, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("Sync HTTP $code: $text")
            return text
        } finally {
            conn.disconnect()
        }
    }
}

/** Per-component aggregate score shown in the progress screen (unsimplified fraction). */
data class ComponentScore(val component: Component, val attempts: Int, val correct: Int) {
    val fraction: String get() = "$correct/$attempts"
}

/** The screen the user can navigate to. */
enum class AppScreen { STACK, PROGRESS }
