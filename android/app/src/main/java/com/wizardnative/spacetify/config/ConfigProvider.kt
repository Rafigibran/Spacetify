package com.wizardnative.spacetify.config

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.wizardnative.spacetify.data.LibraryStore

/**
 * Serves the manager's configuration to the Spacetify module running inside
 * Spotify's process.
 *
 * Two read channels exist because one size does not fit all setups:
 *  1. This provider (primary): works with LSPosed and with LSPatch, as long
 *     as the manager app is installed; Spotify's process simply queries it.
 *  2. XSharedPreferences (fallback inside the module): used when the
 *     provider cannot be reached.
 *
 * URIs:
 *  - content://com.wizardnative.spacetify.config/config   -> columns key, value
 *  - content://com.wizardnative.spacetify.config/trashbin -> columns kind, value
 *    (kind is "track" or "artist")
 */
class ConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        return when (uri.lastPathSegment) {
            "config" -> {
                val cursor = MatrixCursor(arrayOf("key", "value"))
                for ((key, value) in ConfigStore(ctx).all()) {
                    cursor.addRow(arrayOf(key, value))
                }
                cursor
            }
            "trashbin" -> {
                val store = LibraryStore(ctx)
                val cursor = MatrixCursor(arrayOf("kind", "value"))
                for (track in store.blockedTracks()) cursor.addRow(arrayOf("track", track))
                for (artist in store.blockedArtists()) cursor.addRow(arrayOf("artist", artist))
                cursor
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.spacetify"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
