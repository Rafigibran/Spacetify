package com.wizardnative.spacetify.xposed

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

/**
 * The module's `Spicetify.Player.next()`.
 *
 * Desktop extensions call into xpui's player object. On Android the reliable,
 * version-independent way to advance Spotify's playback is the media key
 * route: Spotify is the active media session, so a MEDIA_NEXT key event
 * dispatched through AudioManager reaches it the same way a headset button
 * press does, without touching obfuscated player classes.
 */
object SkipController {
    fun next(context: Context) {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val now = System.currentTimeMillis()
            audio.dispatchMediaKeyEvent(
                KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0)
            )
            audio.dispatchMediaKeyEvent(
                KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0)
            )
        } catch (e: Exception) {
            // Playback control is best-effort; a failed skip must never crash Spotify.
        }
    }
}
