package com.wizardnative.spacetify

import com.wizardnative.spacetify.lyrics.LrcParser
import com.wizardnative.spacetify.lyrics.LrclibClient
import com.wizardnative.spacetify.lyrics.LrclibJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {

    @Test
    fun parsesSyncedLrc() {
        val lrc = """
            [00:01.00]Baris pertama
            [01:02.50]Baris kedua
            [ti:Bukan lirik]
            [02:03.123]Baris ketiga
        """.trimIndent()
        val lines = LrcParser.parseSynced(lrc)
        assertEquals(3, lines.size)
        assertEquals(1_000L, lines[0].timeMs)
        assertEquals("Baris pertama", lines[0].text)
        assertEquals(62_500L, lines[1].timeMs)
        assertEquals(123_123L, lines[2].timeMs)
    }

    @Test
    fun parsesLrclibResponse() {
        val json = """{"id":1,"trackName":"Kangen","instrumental":false,""" +
            """"plainLyrics":"line one\nline two",""" +
            """"syncedLyrics":"[00:01.00]line one\n[00:02.00]line two"}"""
        val result = LrclibJson.parseResponse(json)
        assertTrue(result.found)
        assertFalse(result.instrumental)
        assertEquals(listOf("line one", "line two"), result.plain)
        assertEquals(2, result.synced.size)
    }

    @Test
    fun instrumentalShortCircuits() {
        val result = LrclibJson.parseResponse("""{"instrumental":true,"plainLyrics":null}""")
        assertTrue(result.found)
        assertTrue(result.instrumental)
    }

    @Test
    fun buildUrlMatchesDesktopProvider() {
        val url = LrclibClient.buildUrl("Kangen", "Dewa 19", "Bintang Lima", 240)
        assertTrue(url.startsWith("https://lrclib.net/api/get?"))
        assertTrue(url.contains("track_name=Kangen"))
        assertTrue(url.contains("artist_name=Dewa+19"))
        assertTrue(url.contains("duration=240"))
    }
}
