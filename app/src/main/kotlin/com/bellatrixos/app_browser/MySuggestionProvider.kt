package com.bellatrixos.app_browser

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

/** Penyedia saran pencarian browser. */
class MySuggestionProvider : ContentProvider() {

    /* Inner class MySuggestionProvider$1 (nama kelas tidak valid). */
    inner class AnonymousClass1(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(sQLiteDatabase: SQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE suggestions (_id INTEGER PRIMARY KEY AUTOINCREMENT, term TEXT NOT NULL UNIQUE, last_used INTEGER NOT NULL);")
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_suggestions_term ON suggestions(term);")
            sQLiteDatabase.execSQL("CREATE INDEX IF NOT EXISTS idx_suggestions_last_used ON suggestions(last_used);")
        }

        override fun onDowngrade(sQLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS suggestions")
            onCreate(sQLiteDatabase)
        }

        override fun onUpgrade(sQLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS suggestions")
            onCreate(sQLiteDatabase)
        }
    }

    private var dbHelper: SQLiteOpenHelper? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (matcher.match(uri) != SUGGESTIONS) {
            throw IllegalArgumentException("Invalid delete URI $uri")
        }
        val count = dbHelper!!.writableDatabase.delete(TABLE, selection, selectionArgs)
        val context = context
        if (context != null) {
            context.contentResolver.notifyChange(URI_SEARCH, null)
            context.contentResolver.notifyChange(URI_SUGGESTIONS, null)
        }
        return count
    }

    override fun getType(uri: Uri): String? {
        return when (matcher.match(uri)) {
            SEARCH_SUGGEST -> "vnd.android.cursor.dir/vnd.com.bellatrixos.app_browser.MySuggestionProvider.search_suggest"
            SUGGESTIONS -> "vnd.android.cursor.dir/vnd.com.bellatrixos.app_browser.MySuggestionProvider.suggestion"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (matcher.match(uri) != SUGGESTIONS) {
            throw IllegalArgumentException("Invalid insert URI $uri")
        }
        val contentValues = values ?: throw IllegalArgumentException("Missing 'term' in ContentValues")
        if (!contentValues.containsKey(COL_TERM)) {
            throw IllegalArgumentException("Missing 'term' in ContentValues")
        }
        if (!contentValues.containsKey(COL_LAST_USED)) {
            contentValues.put(COL_LAST_USED, java.lang.Long.valueOf(System.currentTimeMillis()))
        }
        val id = dbHelper!!.writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        val result = Uri.withAppendedPath(uri, id.toString())
        val context = context
        if (context != null) {
            context.contentResolver.notifyChange(URI_SEARCH, null)
            context.contentResolver.notifyChange(URI_SUGGESTIONS, null)
        }
        return result
    }

    override fun onCreate(): Boolean {
        dbHelper = AnonymousClass1(context)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val readableDatabase = dbHelper!!.readableDatabase
        val cursor = when (matcher.match(uri)) {
            SEARCH_SUGGEST -> {
                val term = if (selectionArgs != null && selectionArgs.isNotEmpty()) selectionArgs[0] else ""
                readableDatabase.query(
                    TABLE,
                    arrayOf("_id AS _id", "term AS suggest_text_1"),
                    "term LIKE ?",
                    arrayOf("%$term%"),
                    null,
                    null,
                    "last_used DESC",
                    "5"
                )
            }
            SUGGESTIONS -> readableDatabase.query(
                TABLE,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        val context = context
        if (context != null) {
            cursor.setNotificationUri(context.contentResolver, uri)
        }
        return cursor
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if (matcher.match(uri) != SUGGESTIONS) {
            throw IllegalArgumentException("Invalid update URI $uri")
        }
        val count = dbHelper!!.writableDatabase.update(TABLE, values, selection, selectionArgs)
        val context = context
        if (context != null) {
            context.contentResolver.notifyChange(URI_SEARCH, null)
            context.contentResolver.notifyChange(URI_SUGGESTIONS, null)
        }
        return count
    }

    companion object {
        const val AUTHORITY: String = "com.bellatrixos.app_browser.MySuggestionProvider"
        private const val COL_ID: String = "_id"
        private const val COL_LAST_USED: String = "last_used"
        private const val COL_TERM: String = "term"
        private const val DB_NAME: String = "suggestions.db"
        private const val DB_VERSION: Int = 2
        private const val SEARCH_SUGGEST: Int = 1
        private const val SUGGESTIONS: Int = 2
        private const val TABLE: String = "suggestions"

        @JvmField
        val URI_SEARCH: Uri = Uri.parse("content://com.bellatrixos.app_browser.MySuggestionProvider/search_suggest_query")

        @JvmField
        val URI_SUGGESTIONS: Uri = Uri.parse("content://com.bellatrixos.app_browser.MySuggestionProvider/suggestions")

        private val matcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "search_suggest_query", SEARCH_SUGGEST)
            addURI(AUTHORITY, TABLE, SUGGESTIONS)
        }
    }
}
