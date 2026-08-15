package com.mcai.faketime

import android.content.Context
import android.content.SharedPreferences

/**
 * Module configuration.
 *
 * The companion app writes config through the normal SharedPreferences API
 * (same process). The hook engine running inside every target app process
 * reads the SAME xml file via LSPosed's XSharedPreferences (cross-process).
 *
 * Live reload: the engine re-reads with a short cache window so an offset
 * change takes effect quickly in already-running target processes.
 */
object Config {

    const val PREFS_NAME = "faketime_config"

    private const val KEY_ENABLED = "enabled"
    private const val KEY_OFFSET_MILLIS = "offset_millis"
    private const val KEY_REAL_TIME_APPS = "real_time_apps"

    // Hard safety list: these processes must never be hooked with fake time.
    // system_server handles alarms/sync/boot; LSPosed + our own app must see truth.
    val SAFETY_EXCLUDE = setOf(
        "android",
        "com.android.systemui",
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "com.mcai.faketime",
    )

    fun isExcludedProcess(packageName: String): Boolean =
        packageName in SAFETY_EXCLUDE

    // ---- App side (companion) -----------------------------------------

    fun appPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = appPrefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        appPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun offsetMillis(context: Context): Long =
        appPrefs(context).getLong(KEY_OFFSET_MILLIS, 0L)

    fun setOffsetMillis(context: Context, millis: Long) {
        appPrefs(context).edit().putLong(KEY_OFFSET_MILLIS, millis).apply()
    }

    fun realTimeApps(context: Context): Set<String> =
        appPrefs(context).getStringSet(KEY_REAL_TIME_APPS, emptySet()) ?: emptySet()

    fun setRealTimeApp(context: Context, packageName: String, real: Boolean) {
        val current = appPrefs(context).getStringSet(KEY_REAL_TIME_APPS, emptySet())
            ?: emptySet()
        val updated = current.toMutableSet()
        if (real) updated.add(packageName) else updated.remove(packageName)
        appPrefs(context).edit().putStringSet(KEY_REAL_TIME_APPS, updated).apply()
    }
}
