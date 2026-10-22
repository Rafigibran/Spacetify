package com.wizardnative.spacetify.config

/**
 * The Android counterpart of the desktop `config-xpui.ini`.
 *
 * Desktop sections map like this:
 *  - [Setting]           -> theme + color scheme + inject toggles
 *  - [Preprocesses]      -> always-on in the module (no file patching exists
 *                           on Android, so there is nothing to toggle)
 *  - [AdditionalOptions] -> enabled extensions
 *
 * Values are stored as strings ("1"/"0" for switches) in SharedPreferences
 * named [PREFS_NAME], mirroring the ini file so the module can also read
 * them through XSharedPreferences as a fallback channel.
 */
object SpicetifyConfig {
    const val PREFS_NAME = "spacetify_config"
    const val AUTHORITY = "com.wizardnative.spacetify.config"
    const val SPOTIFY_PACKAGE = "com.spotify.music"
    const val MODULE_PACKAGE = "com.wizardnative.spacetify"

    // [Setting]
    const val KEY_COLOR_SCHEME = "color_scheme"        // desktop: color_scheme
    const val KEY_REPLACE_COLORS = "replace_colors"    // desktop: replace_colors
    const val KEY_INJECT_EXTENSIONS = "inject_extensions" // desktop: extensions (master switch)

    // [AdditionalOptions] extensions
    const val KEY_EXT_TRASHBIN = "ext_trashbin"
    const val KEY_EXT_SKIP_EPISODES = "ext_auto_skip_episodes"

    val defaults: Map<String, String> = linkedMapOf(
        KEY_COLOR_SCHEME to "purple",
        KEY_REPLACE_COLORS to "1",
        KEY_INJECT_EXTENSIONS to "1",
        KEY_EXT_TRASHBIN to "1",
        KEY_EXT_SKIP_EPISODES to "0"
    )
}

/** Reads a "1"/"0" switch from a resolved config map, falling back to defaults. */
fun Map<String, String>.configBool(key: String): Boolean =
    (this[key] ?: SpicetifyConfig.defaults[key] ?: "0") == "1"

/** The configured color scheme name from a resolved config map. */
fun Map<String, String>.configSchemeName(): String =
    this[SpicetifyConfig.KEY_COLOR_SCHEME]
        ?: SpicetifyConfig.defaults.getValue(SpicetifyConfig.KEY_COLOR_SCHEME)
