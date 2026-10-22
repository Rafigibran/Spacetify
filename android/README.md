# Spacetify Android

Versi Android dari Spicetify (repo desktop: `Rafigibran/Spacetify`), dibuat
agar kustomisasi Spicetify berjalan di aplikasi Spotify Android
(`com.spotify.music`).

Satu APK ini berisi dua hal:

1. **Aplikasi manajer** (pengganti CLI desktop): deteksi Spotify, backup APK
   Spotify asli, pilih tema dan skema warna, kelola ekstensi, TrashBin,
   Bookmark, dan Lirik.
2. **Modul LSPosed/Xposed** yang berjalan di dalam proses Spotify dan
   menerapkan tema serta ekstensi di sana. Untuk HP tanpa root, modul yang
   sama bisa disematkan ke Spotify memakai **LSPatch**.

## Kenapa bukan patch file seperti desktop?

Spotify desktop adalah aplikasi web (xpui) sehingga Spicetify bisa me-patch
file JS/CSS-nya. Spotify Android adalah aplikasi native: tidak ada xpui,
tidak ada CSS, dan APK-nya bertanda tangan. Mekanisme yang setara di Android
adalah modul runtime. Penjelasan lengkap hasil studi kode desktop dan peta
portingnya ada di [docs/PORTING_NOTES.md](docs/PORTING_NOTES.md).

## Fitur versi 1

- **Tema warna**: 6 skema bawaan disalin persis dari `color.ini` tema
  SpicetifyDefault desktop (green-dark, nord-light, nord-dark, pink-white,
  purple, dracula). Pewarnaan ulang memakai pemetaan palet warna Spotify di
  level View, jadi tidak bergantung nama class hasil obfuscation dan tetap
  bekerja lintas versi Spotify. Ganti skema di manajer, Spotify berubah
  dalam beberapa detik tanpa restart.
- **TrashBin**: blokir lagu atau artis, otomatis di-skip saat diputar.
- **Auto Skip Episode**: padanan autoSkipVideo untuk podcast, episode
  otomatis di-skip.
- **Bookmark**: simpan lagu yang sedang diputar, buka lagi di Spotify
  dengan satu ketuk.
- **Lirik**: provider LRCLIB dari lyrics-plus (lirik sinkron dan plain).
- **Backup dan Restore**: salin APK Spotify asli sebelum patch LSPatch,
  sebagai jalan pulang.

Belum tersedia, dengan alasan teknis yang ditulis apa adanya di
[docs/PORTING_NOTES.md](docs/PORTING_NOTES.md): auto skip lagu explicit,
keyboard shortcut, loopy loop, shuffle+, full app display, custom apps
new-releases dan reddit.

## Instalasi

Ringkasnya ada dua jalur, dan panduan langkah demi langkahnya juga ada di
dalam aplikasi (menu Panduan Instalasi):

- **LSPosed (root)**: pasang APK ini, aktifkan modul Spacetify di LSPosed
  dengan scope Spotify, force stop Spotify, selesai.
- **LSPatch (tanpa root)**: backup APK Spotify dari aplikasi ini, patch
  Spotify di LSPatch dengan modul Spacetify disematkan, hapus Spotify asli,
  pasang hasil patch, login ulang.

Satu syarat dari Spotify sendiri: untuk fitur yang membaca lagu yang sedang
diputar (Bookmark, Lirik, TrashBin dari lagu berjalan), aktifkan
**Device Broadcast Status** di Pengaturan Spotify.

## Build

Tata letak di repo `Rafigibran/Spacetify`: proyek ini ada di branch
`android`, di dalam folder `android/`, agar kode desktop di `main` tidak
tersentuh. Workflow CI ada di `.github/workflows/android-ci.yml` pada root
repo branch tersebut.

Butuh JDK 17 dan Android SDK platform 34.

```bash
./gradlew testDebugUnitTest   # uji logika murni: parser color.ini, palet, engine ekstensi, LRCLIB
./gradlew assembleDebug       # APK debug
./gradlew assembleRelease     # APK release (ditandatangani debug untuk sideload)
```

GitHub Actions menjalankan hal yang sama pada setiap push ke branch
`android` dan mengunggah APK sebagai artifact.

Paket: `com.wizardnative.spacetify`, minSdk 26, targetSdk 34, Kotlin.

## Struktur kode

```
app/src/main/java/com/wizardnative/spacetify/
  config/     ConfigStore, ConfigProvider, SpacetifyConfig (padanan config-xpui.ini)
  theme/      ColorScheme (parser color.ini + 6 skema), SpotifyPalette (peta warna)
  player/     SpotifyBroadcasts, BroadcastParser, ExtensionEngine, SpotifyReceiver
  lyrics/     LrclibClient, LrcParser (port provider LRCLIB lyrics-plus)
  data/       LibraryStore (TrashBin dan Bookmark)
  spotify/    SpotifyStatus (deteksi dan backup APK)
  xposed/     MainHook, ThemeHook, RemoteConfig, SkipController
  ui/         MainActivity, TrashbinActivity, BookmarksActivity, LyricsActivity, GuideActivity
```

## Lisensi

Mengikuti lisensi repo induk: GNU LGPL-2.1 (lihat file `LICENSE`).
Spicetify adalah proyek komunitas dan tidak berafiliasi dengan Spotify AB.
Gunakan dengan risiko sendiri; memodifikasi aplikasi pihak ketiga dapat
melanggar ketentuan layanan aplikasi tersebut.
