package com.wizardnative.spacetify.player

/**
 * Spotify Android's own playback broadcasts.
 *
 * The desktop wrapper listens to `Spicetify.Player` "songchange" events from
 * inside xpui. The Android equivalent that does not depend on Spotify's
 * obfuscated internals is the set of broadcasts the Spotify app itself sends:
 *
 *  - `com.spotify.music.metadatachanged`
 *  - `com.spotify.music.playbackstatechanged`
 *  - `com.spotify.music.queuechanged`
 *
 * Note: for *other* apps to receive these, the user must enable
 * "Device Broadcast Status" in Spotify Settings. The Spacetify module
 * registers its receiver inside Spotify's own process, and the manager app
 * also listens while it is open; the parsing here is shared and pure so it
 * can be unit tested.
 */
object SpotifyBroadcasts {
    const val ACTION_METADATA = "com.spotify.music.metadatachanged"
    const val ACTION_PLAYBACK_STATE = "com.spotify.music.playbackstatechanged"
    const val ACTION_QUEUE = "com.spotify.music.queuechanged"

    val ACTIONS = listOf(ACTION_METADATA, ACTION_PLAYBACK_STATE, ACTION_QUEUE)
}

data class TrackInfo(
    val uri: String,
    val artist: String,
    val album: String,
    val title: String,
    val durationMs: Long
) {
    /** Stable human key, used by TrashBin when a URI is not available. */
    val artistTitleKey: String get() = "${artist.trim().lowercase()}|${title.trim().lowercase()}"

    val isEpisode: Boolean
        get() = uri.startsWith("spotify:episode:") || uri.startsWith("spotify:show:")

    val isTrack: Boolean get() = uri.startsWith("spotify:track:")
}

data class PlaybackState(
    val playing: Boolean,
    val positionMs: Int
)

object BroadcastParser {
    /**
     * Extras of `metadatachanged` (per Spotify's broadcast documentation):
     * `id` (track URI), `artist`, `album`, `track`, `length` (seconds).
     */
    fun parseMetadata(extras: Map<String, Any?>): TrackInfo? {
        val uri = extras["id"] as? String ?: return null
        if (uri.isBlank()) return null
        val lengthSeconds = when (val len = extras["length"]) {
            is Number -> len.toLong()
            is String -> len.toLongOrNull() ?: 0L
            else -> 0L
        }
        return TrackInfo(
            uri = uri,
            artist = extras["artist"] as? String ?: "",
            album = extras["album"] as? String ?: "",
            title = extras["track"] as? String ?: "",
            durationMs = if (lengthSeconds > 86_400) lengthSeconds else lengthSeconds * 1000
        )
    }

    /** Extras of `playbackstatechanged`: `playing`, `playbackPosition` (ms). */
    fun parsePlaybackState(extras: Map<String, Any?>): PlaybackState {
        val playing = extras["playing"] as? Boolean ?: false
        val position = when (val pos = extras["playbackPosition"]) {
            is Number -> pos.toInt()
            is String -> pos.toIntOrNull() ?: 0
            else -> 0
        }
        return PlaybackState(playing = playing, positionMs = position)
    }
}
