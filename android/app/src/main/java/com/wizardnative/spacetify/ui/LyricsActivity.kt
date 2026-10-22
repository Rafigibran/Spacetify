package com.wizardnative.spacetify.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wizardnative.spacetify.R
import com.wizardnative.spacetify.lyrics.LrclibClient
import java.util.concurrent.Executors

/** Lyrics screen: lyrics-plus, LRCLIB provider only (see Lyrics.kt for why). */
class LyricsActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lyrics)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
        val album = intent.getStringExtra(EXTRA_ALBUM) ?: ""
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)

        val tvTrack = findViewById<TextView>(R.id.tvLyricsTrack)
        val tvLyrics = findViewById<TextView>(R.id.tvLyrics)
        val progress = findViewById<ProgressBar>(R.id.pbLyrics)
        val button = findViewById<Button>(R.id.btnFetchLyrics)

        tvTrack.text = if (title.isBlank()) "Tidak ada lagu" else "$title - $artist"
        if (title.isBlank()) {
            button.isEnabled = false
            tvLyrics.text = "Putar lagu di Spotify dulu, lalu buka layar ini dari tombol Lihat Lirik."
            return
        }

        button.setOnClickListener {
            progress.visibility = View.VISIBLE
            tvLyrics.text = ""
            io.execute {
                val result = LrclibClient.fetch(title, artist, album, durationMs)
                runOnUiThread {
                    progress.visibility = View.GONE
                    tvLyrics.text = when {
                        result.instrumental -> "Lagu instrumental, tidak ada lirik."
                        result.synced.isNotEmpty() ->
                            result.synced.joinToString("\n") { it.text }
                        result.plain.isNotEmpty() ->
                            result.plain.joinToString("\n")
                        else ->
                            "Lirik tidak ditemukan di LRCLIB untuk lagu ini."
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM = "album"
        const val EXTRA_DURATION_MS = "duration_ms"
    }
}
