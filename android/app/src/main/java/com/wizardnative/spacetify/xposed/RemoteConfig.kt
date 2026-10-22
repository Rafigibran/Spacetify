package com.wizardnative.spacetify.xposed

import android.content.Context
import android.net.Uri
import com.wizardnative.spacetify.config.SpicetifyConfig
import com.wizardnative.spacetify.config.configBool
import com.wizardnative.spacetify.config.configSchemeName
import com.wizardnative.spacetify.player.EngineConfig
import com.wizardnative.spacetify.theme.ColorScheme
import com.wizardnative.spacetify.theme.DefaultSchemes
import de.robv.android.xposed.XSharedPreferences

/** Fully resolved configuration as the module inside Spotify sees it. */
data class ModuleConfig(
    val replaceColors: Boolean,
    val scheme: ColorScheme?,
    val extensionsEnabled: Boolean,
    val engine: EngineConfig
)

/**
 * Reads the manager's configuration from inside Spotify's process.
 *
 * Primary channel: the manager's ContentProvider. Fallback:
 * XSharedPreferences (the same `spacetify_config` file the ini file maps to).
 * Results are cached for a few seconds so theme hooks stay cheap, while
 * changes in the manager still apply without restarting Spotify: the Android
 * counterpart of the desktop `watch` / `refresh` commands.
 */
object RemoteConfig {
    private const val TTL_MS = 5_000L

    @Volatile
    private var cached: ModuleConfig? = null

    @Volatile
    private var loadedAt: Long = 0

    fun get(context: Context): ModuleConfig {
        val now = System.currentTimeMillis()
        cached?.let { if (now - loadedAt < TTL_MS) return it }
        val fresh = load(context)
        cached = fresh
        loadedAt = now
        return fresh
    }

    private fun load(context: Context): ModuleConfig {
        var values: Map<String, String> = queryProvider(context)
        if (values.isEmpty()) values = readXShared()
        val trash = queryTrashbin(context)

        val scheme = if (values.configBool(SpicetifyConfig.KEY_REPLACE_COLORS)) {
            DefaultSchemes.byName(values.configSchemeName()) ?: DefaultSchemes.default
        } else {
            null
        }
        return ModuleConfig(
            replaceColors = values.configBool(SpicetifyConfig.KEY_REPLACE_COLORS),
            scheme = scheme,
            extensionsEnabled = values.configBool(SpicetifyConfig.KEY_INJECT_EXTENSIONS),
            engine = EngineConfig(
                trashbinEnabled = values.configBool(SpicetifyConfig.KEY_EXT_TRASHBIN),
                autoSkipEpisodes = values.configBool(SpicetifyConfig.KEY_EXT_SKIP_EPISODES),
                blockedTracks = trash.first,
                blockedArtists = trash.second
            )
        )
    }

    private fun queryProvider(context: Context): Map<String, String> {
        val out = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse("content://${SpicetifyConfig.AUTHORITY}/config")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val keyIdx = cursor.getColumnIndex("key")
                val valueIdx = cursor.getColumnIndex("value")
                if (keyIdx < 0 || valueIdx < 0) return out
                while (cursor.moveToNext()) {
                    val key = cursor.getString(keyIdx) ?: continue
                    out[key] = cursor.getString(valueIdx) ?: ""
                }
            }
        } catch (e: Exception) {
            // Provider unreachable (manager not installed, LSPatch-only setup):
            // the caller falls back to XSharedPreferences.
        }
        return out
    }

    private fun queryTrashbin(context: Context): Pair<Set<String>, Set<String>> {
        val tracks = mutableSetOf<String>()
        val artists = mutableSetOf<String>()
        try {
            val uri = Uri.parse("content://${SpicetifyConfig.AUTHORITY}/trashbin")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val kindIdx = cursor.getColumnIndex("kind")
                val valueIdx = cursor.getColumnIndex("value")
                if (kindIdx < 0 || valueIdx < 0) return Pair(tracks, artists)
                while (cursor.moveToNext()) {
                    val value = cursor.getString(valueIdx) ?: continue
                    when (cursor.getString(kindIdx)) {
                        "track" -> tracks += value
                        "artist" -> artists += value
                    }
                }
            }
        } catch (e: Exception) {
            // No trash data available; extensions simply match nothing.
        }
        return Pair(tracks, artists)
    }

    private fun readXShared(): Map<String, String> {
        return try {
            val prefs = XSharedPreferences(
                SpicetifyConfig.MODULE_PACKAGE,
                SpicetifyConfig.PREFS_NAME
            )
            prefs.makeWorldReadable()
            prefs.reload()
            val out = LinkedHashMap(SpicetifyConfig.defaults)
            for ((key, value) in prefs.all) {
                if (value is String) out[key] = value
            }
            out
        } catch (e: Exception) {
            SpicetifyConfig.defaults
        }
    }
}
