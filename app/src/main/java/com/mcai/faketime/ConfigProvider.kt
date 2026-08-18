package com.mcai.faketime

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Exposes the module config to hooked target processes via the standard
 * ContentProvider IPC. The provider runs in the module's own process (own
 * UID), so any app — regardless of SELinux file-permission limits — can read
 * the current enabled/offset/per-app state. This is the reliable modern
 * replacement for XSharedPreferences (which can silently fail cross-process).
 */
class ConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.mcai.faketime.config"

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val ctx = context
        if (ctx == null) return MatrixCursor(COLUMNS)

        val cursor = MatrixCursor(COLUMNS)
        cursor.addRow(
            arrayOf(
                Config.isEnabled(ctx),
                Config.offsetMillis(ctx),
                Config.realTimeApps(ctx).joinToString("\n"),
            ),
        )
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    companion object {
        const val AUTHORITY = "com.mcai.faketime.provider"
        const val COLUMN_ENABLED = "enabled"
        const val COLUMN_OFFSET = "offset_millis"
        const val COLUMN_REAL_TIME = "real_time_apps"
        val COLUMNS = arrayOf(COLUMN_ENABLED, COLUMN_OFFSET, COLUMN_REAL_TIME)

        fun uri(): Uri = Uri.parse("content://$AUTHORITY/config")
    }
}
