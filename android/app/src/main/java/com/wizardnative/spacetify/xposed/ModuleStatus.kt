package com.wizardnative.spacetify.xposed

/**
 * Self-activation probe, the standard Xposed pattern: this method returns
 * false in a normal install. When a framework (LSPosed / LSPatch) loads the
 * module into the manager's own process, [MainHook] rewrites it to true,
 * which is how the manager knows the module is actually active.
 */
object ModuleStatus {
    @JvmStatic
    fun isActive(): Boolean = false
}
