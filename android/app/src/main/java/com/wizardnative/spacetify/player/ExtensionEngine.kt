package com.wizardnative.spacetify.player

/**
 * Pure decision logic for the ported extensions: the Android counterpart of
 * the `songchange` handlers in the desktop Extensions folder.
 *
 * Ported in v1:
 *  - TrashBin (`Extensions/trashbin.js`): a track whose URI or artist is in
 *    the trash is skipped automatically.
 *  - Auto Skip Episodes (counterpart of `Extensions/autoSkipVideo.js`, which
 *    skips non-music media): Spotify Android identifies podcast episodes by
 *    URI (`spotify:episode:`), so episodes can be skipped reliably.
 *
 * Not ported in v1, deliberately:
 *  - Auto Skip Explicit (`Extensions/autoSkipExplicit.js`): desktop reads
 *    `metadata.is_explicit` from xpui's player state. Neither Spotify's
 *    Android broadcasts nor its MediaSession metadata expose an explicit
 *    flag, so there is no reliable signal on Android yet. Faking it would
 *    skip the wrong songs, so it stays out until a per-version in-process
 *    player hook can read the real flag.
 */
data class EngineConfig(
    val trashbinEnabled: Boolean = true,
    val autoSkipEpisodes: Boolean = false,
    val blockedTracks: Set<String> = emptySet(),
    val blockedArtists: Set<String> = emptySet()
)

sealed class Decision {
    object None : Decision()
    data class Skip(val reason: String) : Decision()
}

object ExtensionEngine {

    fun decide(track: TrackInfo, config: EngineConfig): Decision {
        if (config.trashbinEnabled && isTrashed(track, config)) {
            return Decision.Skip("trashbin")
        }
        if (config.autoSkipEpisodes && track.isEpisode) {
            return Decision.Skip("episode")
        }
        return Decision.None
    }

    fun isTrashed(track: TrackInfo, config: EngineConfig): Boolean {
        if (config.blockedTracks.any { it.equals(track.uri, ignoreCase = true) }) return true
        if (config.blockedTracks.any { it.equals(track.artistTitleKey, ignoreCase = true) }) return true
        val artist = track.artist.trim().lowercase()
        if (artist.isNotEmpty() &&
            config.blockedArtists.any { it.trim().lowercase() == artist }
        ) return true
        return false
    }
}
