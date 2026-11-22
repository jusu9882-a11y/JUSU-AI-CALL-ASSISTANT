package com.jusu.aicallassistant.intelligence

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stores a small, bounded conversation history locally on this device. */
class ChatHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("jusu_chat_history", Context.MODE_PRIVATE)

    fun load(): List<ChatTurn> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val role = item.optString("role")
                val text = item.optString("text")
                if (role !in setOf("user", "assistant") || text.isBlank()) null
                else ChatTurn(role, text, item.optLong("timestamp"))
            }
        }.getOrDefault(emptyList())
    }

    fun append(vararg turns: ChatTurn) {
        val updated = (load() + turns).takeLast(MAX_TURNS)
        val array = JSONArray()
        updated.forEach { turn ->
            array.put(JSONObject().put("role", turn.role).put("text", turn.text).put("timestamp", turn.timestamp))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun clear() { prefs.edit().remove(KEY).apply() }

    private companion object {
        const val KEY = "turns"
        const val MAX_TURNS = 80
    }
}
