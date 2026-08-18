package com.mcai.faketime.hooks

import android.database.Cursor
import com.mcai.faketime.Config
import com.mcai.faketime.ConfigProvider
import de.robv.android.xposed.XposedBridge
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Cross-process config reader used by the hook engine inside each target
 * process.
 *
 * Primary path: query the module's ContentProvider (runs in the module's own
 * process, immune to SELinux/UID file restrictions).
 * Fallback: parse the module's prefs XML directly from disk.
 *
 * Values are cached and reloaded at most every [RELOAD_INTERVAL_MS] so hot
 * paths don't hit IPC/disk on every call.
 */
class HookConfig(private val packageName: String) {

    private var lastLoad = 0L
    private var enabled = true
    private var offsetMillis = 0L
    private var hadConfig = false

    fun offsetForProcess(): Long {
        val now = android.os.SystemClock.uptimeMillis()
        if (!hadConfig || now - lastLoad > RELOAD_INTERVAL_MS) {
            reload()
        }
        if (!enabled) return 0L
        if (Config.isExcludedProcess(packageName)) return 0L
        return offsetMillis
    }

    private fun reload() {
        lastLoad = android.os.SystemClock.uptimeMillis()
        var ok = false

        ok = tryReadFromProvider()
        if (!ok) ok = tryReadFromFile()

        if (!ok) {
            enabled = false
            offsetMillis = 0L
        }
    }

    private fun tryReadFromProvider(): Boolean {
        return try {
            // Hidden API — reach it via reflection (hidden-API enforcement-safe).
            val activityThread = Class.forName("android.app.ActivityThread")
            val app = activityThread.getMethod("currentApplication").invoke(null) as? android.content.Context
                ?: return false
            var cursor: Cursor? = null
            try {
                cursor = app.contentResolver.query(
                    ConfigProvider.uri(),
                    null,
                    null,
                    null,
                    null,
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val enabledIdx = cursor.getColumnIndex(ConfigProvider.COLUMN_ENABLED)
                    val offsetIdx = cursor.getColumnIndex(ConfigProvider.COLUMN_OFFSET)
                    val realIdx = cursor.getColumnIndex(ConfigProvider.COLUMN_REAL_TIME)
                    enabled = enabledIdx >= 0 && cursor.getInt(enabledIdx) == 1
                    offsetMillis = if (offsetIdx >= 0) cursor.getLong(offsetIdx) else 0L
                    if (realIdx >= 0) {
                        val realTime = cursor.getString(realIdx)?.split("\n").orEmpty().toSet()
                        if (packageName in realTime) offsetMillis = 0L
                    }
                    hadConfig = true
                    true
                } else {
                    false
                }
            } finally {
                cursor?.close()
            }
        } catch (t: Throwable) {
            XposedBridge.log("[FakeTime] provider read failed: ${t.message}")
            false
        }
    }

    private fun tryReadFromFile(): Boolean {
        return try {
            val file = File(DATA_DIR, "shared_prefs/${Config.PREFS_NAME}.xml")
            if (!file.exists()) return false
            val values = parsePrefsFile(file)
            enabled = values["enabled"]?.toBoolean() ?: true
            offsetMillis = values["offset_millis"]?.toLongOrNull() ?: 0L
            val realTime = values["real_time_apps"]?.split("|").orEmpty().toSet()
            if (packageName in realTime) offsetMillis = 0L
            hadConfig = true
            true
        } catch (t: Throwable) {
            XposedBridge.log("[FakeTime] direct prefs read failed: ${t.message}")
            false
        }
    }

    private fun parsePrefsFile(file: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        val sets = root.getElementsByTagName("set")
        for (i in 0 until sets.length) {
            val setEl = sets.item(i) as org.w3c.dom.Element
            val name = setEl.getAttribute("name")
            if (name.isEmpty()) continue
            val strings = setEl.getElementsByTagName("string")
            val values = mutableListOf<String>()
            for (j in 0 until strings.length) {
                values.add(strings.item(j).textContent)
            }
            result[name] = values.joinToString("|")
        }
        val scalars = root.getElementsByTagName("*")
        for (i in 0 until scalars.length) {
            val el = scalars.item(i) as org.w3c.dom.Element
            val name = el.getAttribute("name")
            val value = el.getAttribute("value")
            if (name.isNotEmpty() && value.isNotEmpty() && !result.containsKey(name)) {
                result[name] = value
            }
        }
        return result
    }

    companion object {
        private const val DATA_DIR = "/data/user/0/com.mcai.faketime"
        private const val RELOAD_INTERVAL_MS = 1000L
    }
}
