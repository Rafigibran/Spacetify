package com.wizardnative.spacetify.theme

/**
 * Port of the desktop theme model.
 *
 * Desktop Spicetify reads `Themes/<theme>/color.ini`: each section is a color
 * scheme and each key is one semantic UI color (`--spice-<key>` in the
 * generated `colors.css`). The six schemes below are copied value-for-value
 * from `Themes/SpicetifyDefault/color.ini` in the desktop repository.
 *
 * This class is pure Kotlin (no Android classes) so it can be unit tested on
 * the JVM, exactly like the Go config parser is tested on desktop.
 */
data class ColorScheme(
    val name: String,
    val colors: Map<String, String>
) {
    /** Returns the ARGB int for [key], or null when the scheme has no such key. */
    fun argb(key: String): Int? = colors[key]?.let { parseHexColor(it) }

    /** Returns the ARGB int for the first key that exists in this scheme. */
    fun argbFirst(vararg keys: String): Int? {
        for (key in keys) argb(key)?.let { return it }
        return null
    }

    companion object {
        /** Parses an RRGGBB (or AARRGGBB) hex string into an ARGB int. */
        fun parseHexColor(raw: String): Int? {
            val hex = raw.trim().removePrefix("#")
            return try {
                when (hex.length) {
                    6 -> (0xFF000000L or hex.toLong(16)).toInt()
                    8 -> hex.toLong(16).toInt()
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}

/**
 * Parser for the desktop `color.ini` format:
 * `;` starts a comment, `[name]` starts a scheme, `key = RRGGBB` adds a color.
 */
object ColorIniParser {
    fun parse(text: String): Map<String, ColorScheme> {
        val schemes = linkedMapOf<String, MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith(";")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                val name = line.substring(1, line.length - 1).trim()
                current = schemes.getOrPut(name) { linkedMapOf() }
                continue
            }
            val idx = line.indexOf('=')
            if (idx <= 0 || current == null) continue
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) current[key] = value
        }
        return schemes.mapValues { (name, map) -> ColorScheme(name, map.toMap()) }
    }
}

/** The six schemes from `Themes/SpicetifyDefault/color.ini`, verbatim. */
object DefaultSchemes {
    private fun scheme(name: String, vararg pairs: Pair<String, String>) =
        ColorScheme(name, linkedMapOf(*pairs))

    val all: List<ColorScheme> = listOf(
        scheme(
            "green-dark",
            "text" to "FFFFFF", "subtext" to "DEDEDE", "main" to "2E2837",
            "sidebar" to "2E2837", "player" to "2E2837", "card" to "483B5B",
            "shadow" to "202020", "selected-row" to "CDCDCD", "button" to "00E089",
            "button-active" to "483B5B", "button-disabled" to "535353",
            "tab-active" to "483B5B", "notification" to "00E089",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        ),
        scheme(
            "nord-light",
            "text" to "2E3440", "subtext" to "3B4252", "main" to "ECEFF4",
            "sidebar" to "ECEFF4", "player" to "E5E9F0", "card" to "88C0D0",
            "shadow" to "ECEFF4", "selected-row" to "9EA4AF", "button" to "88C0D0",
            "button-active" to "D8DEE9", "button-disabled" to "C0C0C0",
            "tab-active" to "D8DEE9", "notification" to "88C0D0",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        ),
        scheme(
            "nord-dark",
            "text" to "D8DEE9", "subtext" to "ECEFF4", "main" to "2E3440",
            "sidebar" to "2E3440", "player" to "2E3440", "card" to "3B4252",
            "shadow" to "4C566A", "selected-row" to "E5E9F0", "button" to "5E81AC",
            "button-active" to "434C5E", "button-disabled" to "434C5E",
            "tab-active" to "434C5E", "notification" to "5E81AC",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        ),
        scheme(
            "pink-white",
            "text" to "000000", "subtext" to "3D3D3D", "main" to "FAFAFA",
            "sidebar" to "FAFAFA", "player" to "FAFAFA", "card" to "FE6F61",
            "shadow" to "F0F0F0", "selected-row" to "404040", "button" to "FE6F61",
            "button-active" to "E9E9E9", "button-disabled" to "535353",
            "tab-active" to "E9E9E9", "notification" to "FE6F61",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        ),
        scheme(
            "purple",
            "text" to "FFFFFF", "subtext" to "F0F0F0", "main" to "0A0E14",
            "sidebar" to "0A0E14", "player" to "0A0E14", "card" to "6F3C89",
            "shadow" to "1F1525", "selected-row" to "909090", "button" to "6F3C89",
            "button-active" to "795B84", "button-disabled" to "535353",
            "tab-active" to "795B84", "notification" to "6F3C89",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        ),
        scheme(
            "dracula",
            "text" to "FFFFFF", "subtext" to "D8DEE9", "main" to "282A36",
            "sidebar" to "282A36", "player" to "282A36", "card" to "6272A4",
            "shadow" to "44475A", "selected-row" to "F0F0F0", "button" to "FFB86C",
            "button-active" to "44475A", "button-disabled" to "535353",
            "tab-active" to "44475A", "notification" to "FFB86C",
            "notification-error" to "E22134", "misc" to "BFBFBF"
        )
    )

    fun byName(name: String): ColorScheme? = all.firstOrNull { it.name == name }

    val default: ColorScheme = byName("purple") ?: all.first()
}
