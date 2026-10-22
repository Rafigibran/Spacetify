package com.wizardnative.spacetify.theme

/**
 * Android replacement for the desktop `css-map.json` + color replacement.
 *
 * On desktop, preprocessing rewrites Spotify's own color literals in the xpui
 * CSS/JS to `var(--spice-*)`. Spotify Android is a native app, so there is no
 * CSS to rewrite. Instead the module remaps colors *by value* at the Android
 * View layer: when Spotify sets one of its well-known palette colors as a
 * view background or a text color, the value is swapped for the matching
 * semantic color of the active scheme.
 *
 * Matching is exact-value only. That is deliberate: fuzzy matching would
 * recolor album artwork and user images, which desktop Spicetify never does.
 */
object SpotifyPalette {

    // Spotify's stock dark palette (opaque ARGB).
    const val BLACK: Int = 0xFF000000.toInt()        // top bar / deepest background
    const val MAIN: Int = 0xFF121212.toInt()         // main background
    const val ELEVATED: Int = 0xFF181818.toInt()     // player bar / elevated surface
    const val CARD: Int = 0xFF282828.toInt()         // cards
    const val CARD_PRESS: Int = 0xFF2A2A2A.toInt()   // pressed / hover card
    const val BORDER: Int = 0xFF333333.toInt()       // dividers / chips
    const val GREEN: Int = 0xFF1DB954.toInt()        // classic Spotify accent
    const val GREEN_BRIGHT: Int = 0xFF1ED760.toInt() // current Spotify accent
    const val GREEN_DARK: Int = 0xFF1AA34A.toInt()   // pressed accent (approx. stock)
    const val TEXT: Int = 0xFFFFFFFF.toInt()         // primary text
    const val SUBTEXT: Int = 0xFFB3B3B3.toInt()      // secondary text
    const val SUBTEXT_DIM: Int = 0xFFA7A7A7.toInt()  // secondary text (newer UI)
    const val DISABLED: Int = 0xFF6A6A6A.toInt()     // disabled text / icons
    const val ERROR: Int = 0xFFE22134.toInt()        // error red

    /**
     * Builds the remap table for [scheme]: original Spotify color -> themed color.
     * Keys whose scheme value is missing are simply not remapped.
     */
    fun remapTable(scheme: ColorScheme): Map<Int, Int> {
        val table = linkedMapOf<Int, Int>()
        fun put(original: Int, vararg keys: String) {
            scheme.argbFirst(*keys)?.let { table[original] = it }
        }
        put(BLACK, "sidebar", "main")
        put(MAIN, "main")
        put(ELEVATED, "player", "main")
        put(CARD, "card", "main")
        put(CARD_PRESS, "card", "main")
        put(BORDER, "card", "shadow")
        put(GREEN, "button")
        put(GREEN_BRIGHT, "button")
        put(GREEN_DARK, "button-active", "button")
        put(TEXT, "text")
        put(SUBTEXT, "subtext")
        put(SUBTEXT_DIM, "subtext")
        put(DISABLED, "button-disabled", "subtext")
        put(ERROR, "notification-error")
        return table
    }

    /** Returns the themed replacement for [color], or [color] itself when unmapped. */
    fun remap(color: Int, scheme: ColorScheme): Int = remapTable(scheme)[color] ?: color
}
