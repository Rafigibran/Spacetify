package com.wizardnative.spacetify

import com.wizardnative.spacetify.theme.ColorIniParser
import com.wizardnative.spacetify.theme.DefaultSchemes
import com.wizardnative.spacetify.theme.SpotifyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ColorIniParserTest {

    @Test
    fun parsesDesktopColorIniFormat() {
        val ini = """
            ; a comment line
            [test-scheme]
            text = FFFFFF
            main = 121212
            button = 1DB954

            [other]
            text = 000000
        """.trimIndent()
        val schemes = ColorIniParser.parse(ini)
        assertEquals(2, schemes.size)
        assertEquals("FFFFFF", schemes["test-scheme"]?.colors?.get("text"))
        assertEquals(0xFF121212.toInt(), schemes["test-scheme"]?.argb("main"))
        assertNull(schemes["test-scheme"]?.argb("missing"))
    }

    @Test
    fun bundledSchemesMatchDesktopValues() {
        // Spot-check exact values from Themes/SpicetifyDefault/color.ini.
        assertEquals(6, DefaultSchemes.all.size)
        assertEquals(0xFF00E089.toInt(), DefaultSchemes.byName("green-dark")?.argb("button"))
        assertEquals(0xFF282A36.toInt(), DefaultSchemes.byName("dracula")?.argb("main"))
        assertEquals(0xFF5E81AC.toInt(), DefaultSchemes.byName("nord-dark")?.argb("button"))
        assertEquals(0xFFFE6F61.toInt(), DefaultSchemes.byName("pink-white")?.argb("card"))
        assertNotNull(DefaultSchemes.byName("purple"))
        assertNotNull(DefaultSchemes.byName("nord-light"))
    }

    @Test
    fun paletteRemapsSpotifyColorsToScheme() {
        val dracula = DefaultSchemes.byName("dracula")!!
        assertEquals(0xFF282A36.toInt(), SpotifyPalette.remap(SpotifyPalette.MAIN, dracula))
        assertEquals(0xFFFFB86C.toInt(), SpotifyPalette.remap(SpotifyPalette.GREEN, dracula))
        assertEquals(0xFFFFFFFF.toInt(), SpotifyPalette.remap(SpotifyPalette.TEXT, dracula))
        // Unknown colors pass through untouched (album art safety).
        assertEquals(0xFF123456.toInt(), SpotifyPalette.remap(0xFF123456.toInt(), dracula))
    }
}
