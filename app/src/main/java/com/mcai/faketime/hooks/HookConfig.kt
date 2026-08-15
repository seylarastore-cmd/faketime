package com.mcai.faketime.hooks

import com.mcai.faketime.Config
import de.robv.android.xposed.XSharedPreferences

/**
 * Cross-process config reader used by the hook engine inside each target
 * process. Reads the module's prefs xml through LSPosed's XSharedPreferences.
 *
 * Values are cached and reloaded at most every [RELOAD_INTERVAL_MS] so hot
 * paths don't hit the filesystem on every call, but live changes are picked
 * up within the interval.
 */
class HookConfig(private val packageName: String) {

    private var prefs: XSharedPreferences? = null
    private var lastLoad = 0L
    private var enabled = true
    private var offsetMillis = 0L

    fun offsetForProcess(): Long {
        val now = android.os.SystemClock.uptimeMillis()
        if (prefs == null || now - lastLoad > RELOAD_INTERVAL_MS) {
            reload()
        }
        if (!enabled) return 0L
        if (Config.isExcludedProcess(packageName)) return 0L
        return offsetMillis
    }

    private fun reload() {
        lastLoad = android.os.SystemClock.uptimeMillis()
        try {
            // Two-arg constructor: (modulePackage, prefsName). The single-arg
            // variant would look up the *current* (target app) package instead.
            val p = XSharedPreferences(MODULE_PACKAGE, Config.PREFS_NAME)
            p.makeWorldReadable()
            p.reload()
            enabled = p.getBoolean("enabled", true)
            offsetMillis = p.getLong("offset_millis", 0L)
            val realTime = p.getStringSet("real_time_apps", emptySet())
            if (realTime != null && packageName in realTime) offsetMillis = 0L
            prefs = p
        } catch (_: Throwable) {
            // Config unreadable -> module behaves as off (real time).
            enabled = false
            offsetMillis = 0L
        }
    }

    companion object {
        private const val MODULE_PACKAGE = "com.mcai.faketime"
        private const val RELOAD_INTERVAL_MS = 1000L
    }
}
