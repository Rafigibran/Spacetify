package com.wizardnative.spacetify.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wizardnative.spacetify.R

/** Installation guide: LSPosed (root) and LSPatch (non-root) routes. */
class GuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        findViewById<TextView>(R.id.tvGuide).text = GUIDE_TEXT
    }

    companion object {
        private val GUIDE_TEXT = """
            KENAPA BUTUH LSPOSED ATAU LSPATCH?

            Spicetify desktop mem-patch file aplikasi Spotify desktop (xpui.spa).
            Spotify Android adalah aplikasi native, file seperti itu tidak ada,
            jadi patch file ala desktop tidak mungkin dilakukan. Padanan Androidnya:
            modul yang dimuat ke dalam proses Spotify. Itulah yang dilakukan APK ini.
            Satu APK ini berisi aplikasi manajer sekaligus modulnya.

            ------------------------------------------------

            JALUR A: LSPOSED (HP sudah root / Magisk)

            1. Pasang LSPosed dari Magisk seperti modul lain.
            2. Pasang APK Spacetify ini.
            3. Buka LSPosed, aktifkan modul Spacetify, dan pastikan scope-nya
               mencakup Spotify (com.spotify.music).
            4. Force stop Spotify, lalu buka lagi.
            5. Buka Spacetify: status modul harus terbaca AKTIF.
            6. Pilih tema dan ekstensi. Perubahan terbaca modul dalam beberapa
               detik tanpa perlu restart Spotify (padanan perintah refresh/watch
               di desktop).

            ------------------------------------------------

            JALUR B: LSPATCH (tanpa root)

            1. Di Spacetify, ketuk Backup APK Spotify Asli dulu. Backup ini jalan
               pulangmu: simpan baik-baik.
            2. Pasang aplikasi LSPatch (manager) dari rilis resminya.
            3. Di LSPatch, pilih Patch, targetnya aplikasi Spotify, mode Local,
               dan sematkan (embed) modul Spacetify.
            4. LSPatch menghasilkan APK Spotify hasil patch dengan tanda tangan
               berbeda. Karena tanda tangannya beda, Spotify asli harus dihapus
               dulu sebelum APK patch bisa dipasang. Data dan login Spotify akan
               hilang, kamu login ulang setelahnya.
            5. Pasang APK hasil patch, login, lalu atur tema dari aplikasi
               Spacetify seperti biasa.

            Catatan jalur B: Spotify hasil patch tidak lagi menerima update dari
            Play Store. Untuk memperbarui Spotify, ulangi proses patch dengan APK
            Spotify versi baru.

            ------------------------------------------------

            RESTORE (kembali ke Spotify original)

            - Jalur LSPosed: matikan modul Spacetify di LSPosed, force stop
              Spotify. Selesai, Spotify tidak pernah diubah file-nya.
            - Jalur LSPatch: hapus Spotify hasil patch, pasang kembali APK dari
              backup (atau pasang Spotify dari Play Store), lalu login ulang.
              Ini padanan perintah restore di desktop.

            ------------------------------------------------

            DATA PEMUTARAN TIDAK MUNCUL?

            Fitur Bookmark, Lirik, dan deteksi lagu memakai broadcast resmi yang
            dikirim Spotify sendiri. Di Spotify, buka Pengaturan, lalu aktifkan
            "Device Broadcast Status" agar broadcast itu dikirim.

            ------------------------------------------------

            BATASAN JUJUR VERSI 1

            - Tema mewarnai ulang latar dan teks antarmuka Spotify berdasarkan
              palet warna standarnya. Elemen yang digambar sebagai gambar
              (sampul album, banner artis) tidak berubah, sama seperti desktop.
            - Sebagian permukaan Spotify versi terbaru memakai warna di luar
              palet standar; bagian itu tetap warna bawaan sampai petanya
              ditambah di pembaruan berikutnya.
            - Auto skip lagu explicit belum tersedia: desktop membacanya dari
              state internal xpui, Android tidak mengekspos flag explicit lewat
              broadcast maupun MediaSession. Detail teknisnya ada di README repo.
        """.trimIndent()
    }
}
