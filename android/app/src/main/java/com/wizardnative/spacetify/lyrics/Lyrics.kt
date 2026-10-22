package com.wizardnative.spacetify.lyrics

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Port of the lyrics-plus LRCLIB provider
 * (`CustomApps/lyrics-plus/ProviderLRCLIB.js`) and the LRC parsing half of
 * `CustomApps/lyrics-plus/Utils.js` (`parseLocalLyrics`).
 *
 * Desktop lyrics-plus tries Musixmatch, Netease and Genius as well; LRCLIB is
 * ported first because it is the only provider that needs no token, no
 * scraping and no per-region endpoint, which makes it the reliable one on
 * Android.
 */

data class LyricLine(val timeMs: Long, val text: String)

data class LyricsResult(
    val found: Boolean,
    val instrumental: Boolean = false,
    val plain: List<String> = emptyList(),
    val synced: List<LyricLine> = emptyList()
)

object LrcParser {
    private val timeTag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /** Parses LRC text into timed lines, sorted by time. Untimed lines are dropped. */
    fun parseSynced(lrc: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.lines()) {
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) continue
            val text = raw.replace(timeTag, "").trim()
            for (tag in tags) {
                val min = tag.groupValues[1].toLong()
                val sec = tag.groupValues[2].toLong()
                val fracRaw = tag.groupValues[3]
                val fracMs = when (fracRaw.length) {
                    0 -> 0L
                    1 -> fracRaw.toLong() * 100
                    2 -> fracRaw.toLong() * 10
                    else -> fracRaw.take(3).toLong()
                }
                out += LyricLine(min * 60_000 + sec * 1000 + fracMs, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    fun parsePlain(text: String): List<String> =
        text.lines().map { it.trim() }
}

/**
 * Minimal extractor for the flat string/boolean fields of an LRCLIB response.
 * Kept dependency-free (and JVM-testable) instead of pulling in a JSON
 * library for three fields.
 */
object LrclibJson {
    fun stringField(json: String, field: String): String? {
        val key = "\"$field\""
        var idx = json.indexOf(key)
        if (idx < 0) return null
        idx = json.indexOf(':', idx + key.length)
        if (idx < 0) return null
        idx++
        while (idx < json.length && json[idx].isWhitespace()) idx++
        if (idx >= json.length) return null
        if (json.startsWith("null", idx)) return null
        if (json[idx] != '"') return null
        idx++
        val sb = StringBuilder()
        while (idx < json.length) {
            val c = json[idx]
            when {
                c == '\\' && idx + 1 < json.length -> {
                    when (val esc = json[idx + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            if (idx + 5 < json.length) {
                                val code = json.substring(idx + 2, idx + 6).toIntOrNull(16)
                                if (code != null) sb.append(code.toChar())
                                idx += 4
                            }
                        }
                        else -> sb.append(esc)
                    }
                    idx += 2
                }
                c == '"' -> return sb.toString()
                else -> {
                    sb.append(c)
                    idx++
                }
            }
        }
        return sb.toString()
    }

    fun boolField(json: String, field: String): Boolean {
        val key = "\"$field\""
        var idx = json.indexOf(key)
        if (idx < 0) return false
        idx = json.indexOf(':', idx + key.length)
        if (idx < 0) return false
        return json.substring(idx + 1).trimStart().startsWith("true")
    }

    fun parseResponse(json: String): LyricsResult {
        val instrumental = boolField(json, "instrumental")
        if (instrumental) return LyricsResult(found = true, instrumental = true)
        val syncedRaw = stringField(json, "syncedLyrics")
        val plainRaw = stringField(json, "plainLyrics")
        val synced = syncedRaw?.let { LrcParser.parseSynced(it) } ?: emptyList()
        val plain = plainRaw?.let { LrcParser.parsePlain(it) } ?: emptyList()
        return LyricsResult(
            found = synced.isNotEmpty() || plain.isNotEmpty(),
            plain = plain,
            synced = synced
        )
    }
}

object LrclibClient {
    private const val BASE = "https://lrclib.net/api/get"
    const val USER_AGENT = "spacetify-android v1.0.0 (https://github.com/Rafigibran/Spacetify)"

    /** Same query shape as the desktop provider: track, artist, album, duration in seconds. */
    fun buildUrl(title: String, artist: String, album: String, durationSeconds: Long): String {
        fun enc(v: String) = URLEncoder.encode(v, "UTF-8")
        return "$BASE?track_name=${enc(title)}&artist_name=${enc(artist)}" +
            "&album_name=${enc(album)}&duration=$durationSeconds"
    }

    /** Blocking network call: run it off the main thread. */
    fun fetch(title: String, artist: String, album: String, durationMs: Long): LyricsResult {
        val url = URL(buildUrl(title, artist, album, durationMs / 1000))
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("x-user-agent", USER_AGENT)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return LyricsResult(found = false)
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            LrclibJson.parseResponse(body)
        } catch (e: Exception) {
            LyricsResult(found = false)
        } finally {
            conn.disconnect()
        }
    }
}
