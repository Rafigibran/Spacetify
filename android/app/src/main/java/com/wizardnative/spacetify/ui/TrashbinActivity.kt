package com.wizardnative.spacetify.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wizardnative.spacetify.R
import com.wizardnative.spacetify.data.LibraryStore
import com.wizardnative.spacetify.player.CurrentTrack

/** TrashBin manager: port of the desktop extension's settings page. */
class TrashbinActivity : AppCompatActivity() {

    private lateinit var store: LibraryStore
    private lateinit var adapter: ArrayAdapter<String>
    private val displayItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trashbin)
        store = LibraryStore(this)

        val listView = findViewById<ListView>(R.id.lvTrash)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayItems)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = displayItems[position]
            when {
                item.startsWith(PREFIX_TRACK) ->
                    store.removeBlockedTrack(item.removePrefix(PREFIX_TRACK))
                item.startsWith(PREFIX_ARTIST) ->
                    store.removeBlockedArtist(item.removePrefix(PREFIX_ARTIST))
            }
            toast("Dihapus: $item")
            refresh()
        }

        val input = findViewById<EditText>(R.id.etTrashInput)
        findViewById<Button>(R.id.btnAddTrack).setOnClickListener {
            val typed = input.text.toString().trim()
            val current = CurrentTrack.last
            when {
                typed.isNotEmpty() -> store.addBlockedTrack(typed)
                current != null -> store.addBlockedTrack(current.uri)
                else -> {
                    toast("Isi URI lagu, atau putar lagu di Spotify dulu")
                    return@setOnClickListener
                }
            }
            input.text.clear()
            refresh()
        }
        findViewById<Button>(R.id.btnAddArtist).setOnClickListener {
            val typed = input.text.toString().trim()
            val current = CurrentTrack.last
            when {
                typed.isNotEmpty() -> store.addBlockedArtist(typed)
                current != null && current.artist.isNotBlank() ->
                    store.addBlockedArtist(current.artist)
                else -> {
                    toast("Isi nama artis, atau putar lagu artis itu di Spotify dulu")
                    return@setOnClickListener
                }
            }
            input.text.clear()
            refresh()
        }

        refresh()
    }

    private fun refresh() {
        displayItems.clear()
        displayItems += store.blockedTracks().sorted().map { PREFIX_TRACK + it }
        displayItems += store.blockedArtists().sorted().map { PREFIX_ARTIST + it }
        adapter.notifyDataSetChanged()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PREFIX_TRACK = "Lagu: "
        private const val PREFIX_ARTIST = "Artis: "
    }
}
