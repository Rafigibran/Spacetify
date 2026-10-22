package com.wizardnative.spacetify.spotify

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.wizardnative.spacetify.config.SpicetifyConfig
import java.io.File

/**
 * Android counterpart of the desktop CLI's Spotify detection
 * (`FindAppPath` in `src/utils/config.go`) plus the `backup` command:
 * the desktop tool copies Spotify's app files before patching them; here the
 * original Spotify APK is copied aside so a patched (LSPatch) install can
 * always be rolled back to the untouched original.
 */
object SpotifyStatus {

    data class Info(
        val installed: Boolean,
        val versionName: String? = null,
        val versionCode: Long? = null,
        val apkPath: String? = null
    )

    fun detect(context: Context): Info {
        return try {
            val pm = context.packageManager
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    SpicetifyConfig.SPOTIFY_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(SpicetifyConfig.SPOTIFY_PACKAGE, 0)
            }
            Info(
                installed = true,
                versionName = pkg.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                },
                apkPath = pkg.applicationInfo?.sourceDir
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Info(installed = false)
        }
    }

    private fun backupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "backup")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun existingBackup(context: Context): File? =
        backupDir(context).listFiles()?.firstOrNull { it.name.endsWith(".apk") }

    /** Copies the installed Spotify APK into the backup folder. Returns the copy. */
    fun backupApk(context: Context): File? {
        val info = detect(context)
        val source = info.apkPath?.let { File(it) } ?: return null
        if (!source.exists()) return null
        val dest = File(backupDir(context), "spotify-${info.versionName ?: "unknown"}.apk")
        source.copyTo(dest, overwrite = true)
        return dest
    }
}
