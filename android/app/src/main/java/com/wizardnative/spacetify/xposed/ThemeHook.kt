package com.wizardnative.spacetify.xposed

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.wizardnative.spacetify.theme.SpotifyPalette
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runtime theme engine: the Android answer to the desktop pipeline of
 * `preprocess` (rewriting Spotify's color literals) + `apply` (injecting
 * `colors.css`).
 *
 * Instead of patching files, it intercepts the two places where every native
 * Android UI eventually lands its colors: `View.setBackgroundColor` /
 * `View.setBackground` and `TextView.setTextColor`. Colors are remapped by
 * exact value through [SpotifyPalette], so:
 *  - no obfuscated Spotify class or resource name is referenced, which keeps
 *    the hook working across Spotify versions;
 *  - album art and images are untouched, because they are bitmap drawables,
 *    not palette color drawables.
 *
 * The active scheme is re-read from [RemoteConfig] on every call (cached
 * there for a few seconds), so changing the scheme in the manager restyles
 * Spotify without a restart, like desktop `spicetify refresh`.
 */
object ThemeHook {
    private val installed = AtomicBoolean(false)

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        if (!installed.compareAndSet(false, true)) return

        val colorHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val original = param.args.getOrNull(0) as? Int ?: return
                remap(original)?.let { param.args[0] = it }
            }
        }

        try {
            XposedHelpers.findAndHookMethod(
                View::class.java, "setBackgroundColor",
                Int::class.javaPrimitiveType, colorHook
            )
            XposedHelpers.findAndHookMethod(
                TextView::class.java, "setTextColor",
                Int::class.javaPrimitiveType, colorHook
            )
            XposedHelpers.findAndHookMethod(
                TextView::class.java, "setHintTextColor",
                Int::class.javaPrimitiveType, colorHook
            )
            XposedHelpers.findAndHookMethod(
                View::class.java, "setBackground",
                Drawable::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val drawable = param.args.getOrNull(0) as? ColorDrawable ?: return
                        remap(drawable.color)?.let { param.args[0] = ColorDrawable(it) }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Spacetify: theme hook install failed: $e")
        }
    }

    /** Returns the themed color, or null when theming is off / color unmapped. */
    private fun remap(original: Int): Int? {
        val ctx = appContext ?: return null
        val scheme = RemoteConfig.get(ctx).scheme ?: return null
        val mapped = SpotifyPalette.remap(original, scheme)
        return if (mapped != original) mapped else null
    }
}
