package com.wizardnative.spacetify.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wizardnative.spacetify.R
import com.wizardnative.spacetify.data.LibraryStore

/** Bookmark list: port of `Extensions/bookmark.js`. Tap opens Spotify, long-press deletes. */
class BookmarksActivity : AppCompatActivity() {

    private lateinit var store: LibraryStore
    private lateinit var adapter: ArrayAdapter<String>
    private val labels = mutableListOf<String>()
    private var items: List<LibraryStore.Bookmark> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        store = LibraryStore(this)

        val listView = findViewById<ListView>(R.id.lvBookmarks)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val bookmark = items.getOrNull(position) ?: return@setOnItemClickListener
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.uri))
            intent.setPackage("com.spotify.music")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Spotify tidak bisa membuka tautan ini", Toast.LENGTH_SHORT).show()
            }
        }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            val bookmark = items.getOrNull(position) ?: return@setOnItemLongClickListener false
            store.removeBookmark(bookmark.uri)
            Toast.makeText(this, "Bookmark dihapus: ${bookmark.title}", Toast.LENGTH_SHORT).show()
            refresh()
            true
        }

        refresh()
    }

    private fun refresh() {
        items = store.bookmarks()
        labels.clear()
        labels += items.map { "${it.title} - ${it.artist}" }
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvBookmarksEmpty).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
