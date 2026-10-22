package com.wizardnative.spacetify.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wizardnative.spacetify.R
import com.wizardnative.spacetify.config.ConfigStore
import com.wizardnative.spacetify.config.SpicetifyConfig
import com.wizardnative.spacetify.data.LibraryStore
import com.wizardnative.spacetify.player.CurrentTrack
import com.wizardnative.spacetify.player.SpotifyReceiver
import com.wizardnative.spacetify.spotify.SpotifyStatus
import com.wizardnative.spacetify.theme.ColorScheme
import com.wizardnative.spacetify.theme.DefaultSchemes
import com.wizardnative.spacetify.xposed.ModuleStatus
import java.util.concurrent.Executors

/**
 * The manager home screen: the Android counterpart of the desktop CLI.
 * `backup` lives here, `config` is the theme + extension switches below,
 * and `apply`/`refresh` are implicit: the module re-reads this configuration
 * every few seconds, so changes take effect in Spotify without a restart.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var config: ConfigStore
    private lateinit var library: LibraryStore
    private lateinit var tvNowPlaying: TextView
    private lateinit var llSchemePreview: LinearLayout
    private val receiver = SpotifyReceiver { track ->
        runOnUiThread {
            tvNowPlaying.text = "${track.title} - ${track.artist}\n${track.album}"
        }
    }
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config = ConfigStore(this)
        library = LibraryStore(this)

        tvNowPlaying = findViewById(R.id.tvNowPlaying)
        llSchemePreview = findViewById(R.id.llSchemePreview)

        bindStatus()
        bindTheme()
        bindExtensions()
        bindActions()
    }

    override fun onStart() {
        super.onStart()
        SpotifyReceiver.register(this, receiver)
        CurrentTrack.last?.let {
            tvNowPlaying.text = "${it.title} - ${it.artist}\n${it.album}"
        }
        refreshModuleStatus()
    }

    override fun onStop() {
        SpotifyReceiver.unregister(this, receiver)
        super.onStop()
    }

    private fun bindStatus() {
        val info = SpotifyStatus.detect(this)
        findViewById<TextView>(R.id.tvSpotifyStatus).text = if (info.installed) {
            "Spotify: terpasang, versi ${info.versionName ?: "?"}"
        } else {
            "Spotify: TIDAK terpasang (com.spotify.music tidak ditemukan)"
        }
        refreshModuleStatus()
        updateBackupInfo()
    }

    private fun refreshModuleStatus() {
        if (!::config.isInitialized) return
        findViewById<TextView>(R.id.tvModuleStatus).text = if (ModuleStatus.isActive()) {
            "Modul: AKTIF (framework membaca modul ini)"
        } else {
            "Modul: belum aktif. Aktifkan lewat LSPosed, atau patch Spotify dengan LSPatch. Lihat Panduan Instalasi."
        }
    }

    private fun updateBackupInfo() {
        val backup = SpotifyStatus.existingBackup(this)
        findViewById<TextView>(R.id.tvBackupInfo).text = if (backup != null) {
            "Backup tersimpan: ${backup.name} (${backup.length() / 1024} KB)\nLokasi: ${backup.absolutePath}"
        } else {
            "Belum ada backup. Lakukan backup sebelum mem-patch Spotify dengan LSPatch."
        }
    }

    private fun bindTheme() {
        val switchReplace = findViewById<SwitchMaterial>(R.id.switchReplaceColors)
        switchReplace.isChecked = config.bool(SpicetifyConfig.KEY_REPLACE_COLORS)
        switchReplace.setOnCheckedChangeListener { _, checked ->
            config.setBool(SpicetifyConfig.KEY_REPLACE_COLORS, checked)
            toast(if (checked) "Tema aktif, Spotify akan berubah dalam beberapa detik" else "Tema dimatikan")
        }

        val spinner = findViewById<Spinner>(R.id.spinnerScheme)
        val names = DefaultSchemes.all.map { it.name }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val current = config.get(SpicetifyConfig.KEY_COLOR_SCHEME)
        spinner.setSelection(names.indexOf(current).coerceAtLeast(0))
        showPreview(DefaultSchemes.byName(current) ?: DefaultSchemes.default)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val scheme = DefaultSchemes.all[position]
                if (scheme.name != config.get(SpicetifyConfig.KEY_COLOR_SCHEME)) {
                    config.set(SpicetifyConfig.KEY_COLOR_SCHEME, scheme.name)
                }
                showPreview(scheme)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun showPreview(scheme: ColorScheme) {
        llSchemePreview.removeAllViews()
        val size = (28 * resources.displayMetrics.density).toInt()
        for (key in listOf("main", "card", "button", "text", "subtext")) {
            val swatch = View(this)
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = (6 * resources.displayMetrics.density).toInt()
            swatch.layoutParams = params
            swatch.setBackgroundColor(scheme.argb(key) ?: Color.TRANSPARENT)
            swatch.contentDescription = key
            llSchemePreview.addView(swatch)
        }
    }

    private fun bindExtensions() {
        fun bindSwitch(id: Int, key: String) {
            val sw = findViewById<SwitchMaterial>(id)
            sw.isChecked = config.bool(key)
            sw.setOnCheckedChangeListener { _, checked -> config.setBool(key, checked) }
        }
        bindSwitch(R.id.switchExtensions, SpicetifyConfig.KEY_INJECT_EXTENSIONS)
        bindSwitch(R.id.switchTrashbin, SpicetifyConfig.KEY_EXT_TRASHBIN)
        bindSwitch(R.id.switchSkipEpisodes, SpicetifyConfig.KEY_EXT_SKIP_EPISODES)
    }

    private fun bindActions() {
        findViewById<Button>(R.id.btnGuide).setOnClickListener {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            io.execute {
                val file = SpotifyStatus.backupApk(this)
                runOnUiThread {
                    if (file != null) {
                        toast("Backup selesai: ${file.name}")
                        updateBackupInfo()
                    } else {
                        toast("Backup gagal: Spotify tidak terpasang atau APK tidak terbaca")
                    }
                }
            }
        }
        findViewById<Button>(R.id.btnOpenTrashbin).setOnClickListener {
            startActivity(Intent(this, TrashbinActivity::class.java))
        }
        findViewById<Button>(R.id.btnOpenBookmarks).setOnClickListener {
            startActivity(Intent(this, BookmarksActivity::class.java))
        }
        findViewById<Button>(R.id.btnBookmarkNow).setOnClickListener {
            val track = CurrentTrack.last
            if (track == null) {
                toast("Belum ada lagu terbaca. Putar lagu di Spotify dulu.")
            } else {
                library.addBookmark(track)
                toast("Bookmark tersimpan: ${track.title}")
            }
        }
        findViewById<Button>(R.id.btnLyricsNow).setOnClickListener {
            val track = CurrentTrack.last
            if (track == null) {
                toast("Belum ada lagu terbaca. Putar lagu di Spotify dulu.")
            } else {
                startActivity(
                    Intent(this, LyricsActivity::class.java)
                        .putExtra(LyricsActivity.EXTRA_TITLE, track.title)
                        .putExtra(LyricsActivity.EXTRA_ARTIST, track.artist)
                        .putExtra(LyricsActivity.EXTRA_ALBUM, track.album)
                        .putExtra(LyricsActivity.EXTRA_DURATION_MS, track.durationMs)
                )
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
