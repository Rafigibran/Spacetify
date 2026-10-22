package com.wizardnative.spacetify.config

import android.content.Context
import android.content.SharedPreferences

/** Manager-side reader/writer for [SpicetifyConfig] values. */
class ConfigStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(SpicetifyConfig.PREFS_NAME, Context.MODE_PRIVATE)

    fun all(): Map<String, String> {
        val out = LinkedHashMap(SpicetifyConfig.defaults)
        for ((key, value) in prefs.all) {
            if (value is String) out[key] = value
        }
        return out
    }

    fun get(key: String): String = all()[key] ?: SpicetifyConfig.defaults[key] ?: ""

    fun bool(key: String): Boolean = get(key) == "1"

    fun set(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun setBool(key: String, value: Boolean) = set(key, if (value) "1" else "0")
}
