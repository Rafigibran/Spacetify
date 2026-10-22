package com.wizardnative.spacetify.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/** Holds the most recent track seen from Spotify's broadcasts. */
object CurrentTrack {
    @Volatile
    var last: TrackInfo? = null

    @Volatile
    var playback: PlaybackState? = null
}

/**
 * Listens to Spotify's playback broadcasts and updates [CurrentTrack].
 * Used by the manager app (while it is open) and by the module inside
 * Spotify's process. [onTrack] fires on every metadata change.
 */
class SpotifyReceiver(
    private val onTrack: (TrackInfo) -> Unit = {}
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val extras = mutableMapOf<String, Any?>()
        intent.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                @Suppress("DEPRECATION")
                extras[key] = bundle.get(key)
            }
        }
        when (intent.action) {
            SpotifyBroadcasts.ACTION_METADATA -> {
                BroadcastParser.parseMetadata(extras)?.let { track ->
                    CurrentTrack.last = track
                    onTrack(track)
                }
            }
            SpotifyBroadcasts.ACTION_PLAYBACK_STATE -> {
                CurrentTrack.playback = BroadcastParser.parsePlaybackState(extras)
            }
        }
    }

    companion object {
        fun register(context: Context, receiver: SpotifyReceiver) {
            val filter = IntentFilter().apply {
                SpotifyBroadcasts.ACTIONS.forEach { addAction(it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
        }

        fun unregister(context: Context, receiver: SpotifyReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Already unregistered; nothing to do.
            }
        }
    }
}
