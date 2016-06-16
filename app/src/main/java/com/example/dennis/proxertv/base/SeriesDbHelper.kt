package com.example.dennis.proxertv.base

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

object SeriesScheme {
    const val TABLE = "mySeries"
    const val ID = "id"
    const val TITLE = "title"
    const val IMAGE = "imageUrl"
}

class SeriesDbHelper(context: Context) : ManagedSQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(SeriesScheme.TABLE, true,
                SeriesScheme.ID to INTEGER + PRIMARY_KEY + UNIQUE,
                SeriesScheme.TITLE to TEXT,
                SeriesScheme.IMAGE to TEXT)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable(SeriesScheme.TABLE, true)
        onCreate(db)
    }

    fun clearDb() {
        use {
            dropTable(SeriesScheme.TABLE, true)
            onCreate(this)
        }
    }

    companion object {
        private const val DB_NAME = "mySeriesList.db"
        private const val DB_VERSION = 1
    }
}