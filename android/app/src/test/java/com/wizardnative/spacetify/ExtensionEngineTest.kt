package com.wizardnative.spacetify

import com.wizardnative.spacetify.player.BroadcastParser
import com.wizardnative.spacetify.player.Decision
import com.wizardnative.spacetify.player.EngineConfig
import com.wizardnative.spacetify.player.ExtensionEngine
import com.wizardnative.spacetify.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionEngineTest {

    private val song = TrackInfo(
        uri = "spotify:track:abc123",
        artist = "Dewa 19",
        album = "Bintang Lima",
        title = "Kangen",
        durationMs = 240_000
    )

    private val episode = TrackInfo(
        uri = "spotify:episode:xyz789",
        artist = "Podcast Ancur",
        album = "",
        title = "Episode 1",
        durationMs = 3_600_000
    )

    @Test
    fun trashbinSkipsBlockedTrackUri() {
        val config = EngineConfig(blockedTracks = setOf("spotify:track:abc123"))
        assertTrue(ExtensionEngine.decide(song, config) is Decision.Skip)
    }

    @Test
    fun trashbinSkipsBlockedArtistCaseInsensitive() {
        val config = EngineConfig(blockedArtists = setOf("dewa 19"))
        assertTrue(ExtensionEngine.decide(song, config) is Decision.Skip)
    }

    @Test
    fun trashbinDisabledMeansNoSkip() {
        val config = EngineConfig(
            trashbinEnabled = false,
            blockedTracks = setOf("spotify:track:abc123")
        )
        assertEquals(Decision.None, ExtensionEngine.decide(song, config))
    }

    @Test
    fun episodesSkippedOnlyWhenEnabled() {
        assertEquals(
            Decision.None,
            ExtensionEngine.decide(episode, EngineConfig(autoSkipEpisodes = false))
        )
        assertTrue(
            ExtensionEngine.decide(episode, EngineConfig(autoSkipEpisodes = true))
                is Decision.Skip
        )
        // Songs are never touched by the episode rule.
        assertEquals(
            Decision.None,
            ExtensionEngine.decide(song, EngineConfig(autoSkipEpisodes = true))
        )
    }

    @Test
    fun parsesSpotifyMetadataBroadcast() {
        val track = BroadcastParser.parseMetadata(
            mapOf(
                "id" to "spotify:track:abc123",
                "artist" to "Dewa 19",
                "album" to "Bintang Lima",
                "track" to "Kangen",
                "length" to 240
            )
        )
        assertEquals("Kangen", track?.title)
        assertEquals(240_000L, track?.durationMs)
        assertEquals(false, track?.isEpisode)
    }
}
