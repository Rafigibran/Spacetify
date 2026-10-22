package com.wizardnative.spacetify.data

import android.content.Context
import com.wizardnative.spacetify.player.TrackInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manager-side storage for the two user libraries:
 *  - TrashBin contents (blocked tracks and artists), port of the desktop
 *    extension's LocalStorage lists.
 *  - Bookmarks, port of `Extensions/bookmark.js` (track + artist + album,
 *    reopenable through the `spotify:` URI).
 */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---- TrashBin -------------------------------------------------------

    fun blockedTracks(): Set<String> =
        prefs.getStringSet(KEY_TRASH_TRACKS, emptySet())?.toSet() ?: emptySet()

    fun blockedArtists(): Set<String> =
        prefs.getStringSet(KEY_TRASH_ARTISTS, emptySet())?.toSet() ?: emptySet()

    fun addBlockedTrack(entry: String) {
        prefs.edit().putStringSet(KEY_TRASH_TRACKS, blockedTracks() + entry).apply()
    }

    fun removeBlockedTrack(entry: String) {
        prefs.edit().putStringSet(KEY_TRASH_TRACKS, blockedTracks() - entry).apply()
    }

    fun addBlockedArtist(artist: String) {
        prefs.edit().putStringSet(KEY_TRASH_ARTISTS, blockedArtists() + artist).apply()
    }

    fun removeBlockedArtist(artist: String) {
        prefs.edit().putStringSet(KEY_TRASH_ARTISTS, blockedArtists() - artist).apply()
    }

    // ---- Bookmarks ------------------------------------------------------

    data class Bookmark(
        val uri: String,
        val title: String,
        val artist: String,
        val album: String,
        val savedAt: Long
    )

    fun bookmarks(): List<Bookmark> {
        val raw = prefs.getString(KEY_BOOKMARKS, "[]") ?: "[]"
        val out = mutableListOf<Bookmark>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out += Bookmark(
                    uri = obj.optString("uri"),
                    title = obj.optString("title"),
                    artist = obj.optString("artist"),
                    album = obj.optString("album"),
                    savedAt = obj.optLong("savedAt")
                )
            }
        } catch (e: Exception) {
            // Corrupted storage must never crash the manager; start clean.
            return emptyList()
        }
        return out.sortedByDescending { it.savedAt }
    }

    fun addBookmark(track: TrackInfo) {
        val current = bookmarks().filterNot { it.uri == track.uri }
        val arr = JSONArray()
        val all = listOf(
            Bookmark(track.uri, track.title, track.artist, track.album, System.currentTimeMillis())
        ) + current
        for (b in all) {
            arr.put(
                JSONObject()
                    .put("uri", b.uri)
                    .put("title", b.title)
                    .put("artist", b.artist)
                    .put("album", b.album)
                    .put("savedAt", b.savedAt)
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun removeBookmark(uri: String) {
        val kept = bookmarks().filterNot { it.uri == uri }
        val arr = JSONArray()
        for (b in kept) {
            arr.put(
                JSONObject()
                    .put("uri", b.uri)
                    .put("title", b.title)
                    .put("artist", b.artist)
                    .put("album", b.album)
                    .put("savedAt", b.savedAt)
            )
        }
        prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "spacetify_library"
        private const val KEY_TRASH_TRACKS = "trash_tracks"
        private const val KEY_TRASH_ARTISTS = "trash_artists"
        private const val KEY_BOOKMARKS = "bookmarks_json"
    }
}
