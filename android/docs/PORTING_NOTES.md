# Catatan Porting: Spicetify Desktop ke Android

Dokumen ini merangkum hasil mempelajari seluruh repo `Rafigibran/Spacetify`
(fork dari spicetify/cli) dan keputusan teknis versi Androidnya.

## 1. Cara kerja versi desktop (hasil studi kode)

Repo desktop (139 file) terdiri dari:

| Bagian | Isi |
| --- | --- |
| `spicetify.go` + `src/cmd/` | CLI Go: perintah `backup`, `apply`, `refresh`, `restore`, `watch`, `config`, `path`, `restart`, `enable-devtools`, `spotify-updates` |
| `src/backup/` | Menyalin file aplikasi Spotify dan mengekstrak `Apps/xpui.spa` (arsip zip berisi aplikasi web Spotify desktop) |
| `src/preprocess/preprocess.go` (1.224 baris) | Me-patch JS/CSS xpui dengan regex: mengekspos API internal Spotify menjadi global `Spicetify.*` lewat pemuatan chunk webpack, menerjemahkan nama class CSS yang ter-obfuscate memakai `css-map.json` (217 KB), mengganti literal warna Spotify menjadi variabel `var(--spice-*)`, mematikan Sentry dan logging, menghapus aturan RTL |
| `src/apply/apply.go` | Menghasilkan `colors.css` dari skema `color.ini`, menyalin `user.css` tema, menyuntikkan file ekstensi JS dan custom apps ke xpui, mendaftarkan custom apps ke navigasi |
| `src/jsHelper/spicetifyWrapper/` | Implementasi API `Spicetify.*`: Player, Platform, CosmosAsync, LocalStorage, Menu, Popup, Topbar, Playbar, ReactComponent, dan lain-lain (permukaan API lengkap di `globals.d.ts`, 62 KB) |
| `Extensions/` | 10 ekstensi JS: autoSkipExplicit, autoSkipVideo, bookmark, fullAppDisplay, keyboardShortcut, loopyLoop, popupLyrics, shuffle+, trashbin, webnowplaying |
| `CustomApps/` | 3 custom apps: lyrics-plus (provider LRCLIB, Musixmatch, Netease, Genius), new-releases, reddit |
| `Themes/SpicetifyDefault/` | `color.ini` berisi 16 kunci warna semantik x 6 skema (green-dark, nord-light, nord-dark, pink-white, purple, dracula) plus `user.css` |
| `src/utils/config.go` | Format `config-xpui.ini`: section Setting, Preprocesses, AdditionalOptions, Patch |

Inti mekanismenya satu kalimat: **Spotify desktop adalah aplikasi web
(Chromium/xpui), jadi ia bisa dibedah sebagai file web, di-patch, lalu
disuntik CSS dan JS.**

## 2. Kenapa mekanisme itu tidak bisa dipakai di Android

Spotify Android (`com.spotify.music`) adalah aplikasi Android native:

- Tidak ada `xpui.spa`, tidak ada halaman web utama, tidak ada webpack chunk
  yang memuat UI. UI digambar oleh View Android native dari kode Kotlin/Java
  yang ter-obfuscate.
- Tidak ada file CSS. Warna adalah resource dan nilai literal di kode native.
- File APK ditandatangani; mengubah isinya membatalkan tanda tangan dan APK
  hasil ubahan tidak bisa dipasang menimpa yang asli.
- Flag metadata yang dipakai ekstensi desktop, misalnya `is_explicit` dari
  state player xpui, tidak diekspos oleh permukaan publik Android (broadcast
  pemutaran Spotify maupun metadata MediaSession tidak memuatnya).

Kesimpulan: porting 1:1 tidak mungkin secara teknis, dan dokumen ini tidak
berpura-pura sebaliknya. Yang diport adalah **konsep dan datanya**, dengan
mekanisme yang memang bekerja di Android.

## 3. Peta porting

| Desktop | Android (repo ini) |
| --- | --- |
| CLI Go (`backup`, `apply`, `config`, `restore`, `watch`) | Aplikasi manajer: layar Status (deteksi Spotify, padanan `FindAppPath`), tombol Backup APK asli (padanan `backup`), saklar tema dan ekstensi (padanan `config`), panduan Restore. `apply`/`refresh` bersifat implisit: modul membaca ulang konfigurasi tiap beberapa detik, jadi perubahan langsung berlaku tanpa restart Spotify |
| Patch file xpui | Modul LSPosed/Xposed di dalam proses Spotify; untuk HP non-root, modul yang sama disematkan ke APK Spotify memakai LSPatch |
| Preprocess: ganti literal warna ke `var(--spice-*)` | `SpotifyPalette` + `ThemeHook`: nilai warna palet standar Spotify dipetakan ulang berdasarkan nilainya di `View.setBackgroundColor`, `View.setBackground`, `TextView.setTextColor`. Pencocokan nilai persis, bukan nama class, jadi tidak bergantung obfuscation dan tidak mewarnai gambar/sampul album |
| `color.ini` (16 kunci x 6 skema) | `ColorScheme.kt`: keenam skema disalin nilai demi nilai; parser format color.ini ikut diport dan diuji |
| `Spicetify.Player` event `songchange` | Broadcast resmi Spotify Android: `com.spotify.music.metadatachanged`, `playbackstatechanged`, `queuechanged`, diparsing oleh `BroadcastParser` |
| `Spicetify.Player.next()` | `SkipController`: event tombol media `KEYCODE_MEDIA_NEXT` lewat AudioManager, jalur yang sama dengan tombol headset |
| Ekstensi trashbin | Diport penuh: daftar blokir lagu (URI) dan artis, dikelola di manajer, keputusan skip di `ExtensionEngine` |
| Ekstensi autoSkipVideo | Diport sebagai Auto Skip Episode: di desktop ia men-skip media bertipe video dari metadata; di Android, episode podcast dikenali dari URI `spotify:episode:` |
| Ekstensi bookmark | Diport: bookmark lagu yang sedang diputar, dibuka lagi lewat URI `spotify:` |
| Custom app lyrics-plus | Provider LRCLIB diport penuh (endpoint, parameter, header, parsing LRC sinkron dan plain). Provider lain belum: Musixmatch dan Genius butuh token/scraping, Netease butuh penanganan region |
| Ekstensi autoSkipExplicit | **Belum diport.** Desktop membaca `metadata.is_explicit` dari state xpui; Android tidak mengekspos flag explicit di broadcast maupun MediaSession. Menebak dari judul atau sumber luar akan men-skip lagu yang salah. Jalur yang benar: hook player internal per versi Spotify, itu pekerjaan riset terpisah per rilis Spotify |
| Ekstensi keyboardShortcut, loopyLoop, fullAppDisplay, shuffle+, webnowplaying, popupLyrics | Belum diport: bertumpu pada keyboard fisik, DOM xpui, atau UI desktop yang tidak punya padanan di aplikasi Android |
| Custom apps new-releases, reddit | Belum diport: keduanya halaman web di dalam xpui; padanan Androidnya adalah layar tersendiri di manajer, masuk peta jalan |
| `restore` | LSPosed: matikan modul, Spotify tidak pernah diubah. LSPatch: pasang kembali APK backup |

## 4. Saluran konfigurasi manajer ke modul

Modul berjalan di proses Spotify (UID berbeda), jadi konfigurasi dibaca lewat
dua saluran, dicoba berurutan:

1. ContentProvider milik manajer (`content://com.wizardnative.spacetify.config/config`
   dan `/trashbin`). Bekerja di LSPosed maupun LSPatch selama aplikasi
   manajer terpasang.
2. XSharedPreferences atas file `spacetify_config`, nama yang sama dengan
   semangat `config-xpui.ini`, sebagai cadangan.

## 5. Risiko dan batasan yang diketahui

- Tema berbasis palet: permukaan Spotify yang memakai warna di luar palet
  standar yang sudah dipetakan tidak akan berubah sampai petanya ditambah.
  Menambah peta itu aman dan lokal sifatnya (satu entri di `SpotifyPalette`).
- Broadcast Spotify untuk aplikasi lain mensyaratkan pengaturan
  "Device Broadcast Status" aktif di Spotify. Di dalam proses Spotify
  sendiri, receiver modul terdaftar sebagai bagian dari aplikasi itu.
- LSPatch membatalkan jalur update Play Store untuk Spotify dan mengharuskan
  hapus-pasang. Ini sifat metode non-root di Android, bukan kekhususan proyek
  ini. Backup APK asli disediakan persis untuk jalan pulang ini.
- Hook tema menangkap pewarnaan di level View. Komponen Spotify yang
  menggambar lewat canvas kustom atau Compose dengan warna hardcoded di luar
  pemanggilan `setBackgroundColor`/`setTextColor` bisa lolos dari pemetaan.
