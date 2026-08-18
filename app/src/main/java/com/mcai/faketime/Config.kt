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
        commit(context) { it.putBoolean(KEY_ENABLED, enabled) }
    }

    fun offsetMillis(context: Context): Long =
        appPrefs(context).getLong(KEY_OFFSET_MILLIS, 0L)

    fun setOffsetMillis(context: Context, millis: Long) {
        commit(context) { it.putLong(KEY_OFFSET_MILLIS, millis) }
    }

    fun realTimeApps(context: Context): Set<String> =
        appPrefs(context).getStringSet(KEY_REAL_TIME_APPS, emptySet()) ?: emptySet()

    fun setRealTimeApp(context: Context, packageName: String, real: Boolean) {
        val current = appPrefs(context).getStringSet(KEY_REAL_TIME_APPS, emptySet())
            ?: emptySet()
        val updated = current.toMutableSet()
        if (real) updated.add(packageName) else updated.remove(packageName)
        commit(context) { it.putStringSet(KEY_REAL_TIME_APPS, updated) }
    }

    /**
     * Write synchronously (commit) so the hooks in other processes can read
     * the file immediately, and chmod it world-readable so LSPosed's
     * XSharedPreferences can open it cross-process.
     */
    private fun commit(context: Context, edit: (SharedPreferences.Editor) -> Unit) {
        val prefs = appPrefs(context)
        edit(prefs.edit()).commit()
        try {
            val file = java.io.File(context.applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml")
            file.setReadable(true, false)
            file.setWritable(true, false)
            file.setExecutable(false, false)
        } catch (_: Throwable) {}
    }
}
