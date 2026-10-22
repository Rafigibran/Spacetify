package com.wizardnative.spacetify.xposed

import android.app.Application
import android.content.Context
import com.wizardnative.spacetify.config.SpicetifyConfig
import com.wizardnative.spacetify.player.Decision
import com.wizardnative.spacetify.player.ExtensionEngine
import com.wizardnative.spacetify.player.SpotifyReceiver
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Xposed entry point (referenced from `assets/xposed_init`).
 *
 * Desktop Spicetify patches Spotify's files once and the patched client then
 * runs the injected code by itself. Android cannot do that safely, so the
 * framework (LSPosed, or LSPatch for non-rooted phones) loads this class
 * into Spotify's process instead, where it:
 *  1. installs the theme hooks ([ThemeHook]);
 *  2. registers a receiver for Spotify's own playback broadcasts and runs
 *     the ported extension decisions ([ExtensionEngine]), skipping through
 *     [SkipController].
 *
 * Everything is best-effort and exception-guarded: a Spacetify failure must
 * never crash or destabilize Spotify itself.
 */
class MainHook : IXposedHookLoadPackage {

    private val initialized = AtomicBoolean(false)

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Self-probe so the manager can show whether the module is active.
        if (lpparam.packageName == SpicetifyConfig.MODULE_PACKAGE) {
            try {
                XposedHelpers.findAndHookMethod(
                    ModuleStatus::class.java.name,
                    lpparam.classLoader,
                    "isActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (e: Throwable) {
                XposedBridge.log("Spacetify: self probe failed: $e")
            }
            return
        }

        if (lpparam.packageName != SpicetifyConfig.SPOTIFY_PACKAGE) return
        if (lpparam.processName != SpicetifyConfig.SPOTIFY_PACKAGE) return

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        initInSpotify(app.applicationContext)
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Spacetify: bootstrap hook failed: $e")
        }
    }

    private fun initInSpotify(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        try {
            ThemeHook.install(context)

            val receiver = SpotifyReceiver { track ->
                try {
                    val config = RemoteConfig.get(context)
                    if (config.extensionsEnabled) {
                        val decision = ExtensionEngine.decide(track, config.engine)
                        if (decision is Decision.Skip) {
                            XposedBridge.log(
                                "Spacetify: skipping ${track.uri} (${decision.reason})"
                            )
                            SkipController.next(context)
                        }
                    }
                } catch (e: Throwable) {
                    XposedBridge.log("Spacetify: extension decision failed: $e")
                }
            }
            SpotifyReceiver.register(context, receiver)
            XposedBridge.log("Spacetify: active in Spotify process")
        } catch (e: Throwable) {
            XposedBridge.log("Spacetify: init failed: $e")
        }
    }
}
