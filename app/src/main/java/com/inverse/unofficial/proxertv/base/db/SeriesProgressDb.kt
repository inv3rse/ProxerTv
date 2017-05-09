package com.inverse.unofficial.proxertv.base.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject

internal object SeriesProgressScheme {
    const val TABLE = "seriesProgress"
    const val ID = "series_id"
    const val LAST_EPISODE = "last_episode"
}

class SeriesProgressDbHelper(context: Context) : ManagedSQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(SeriesProgressScheme.TABLE, true,
                SeriesProgressScheme.ID to INTEGER + PRIMARY_KEY + UNIQUE,
                SeriesProgressScheme.LAST_EPISODE to INTEGER)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.dropTable(SeriesProgressScheme.TABLE, true)
        onCreate(db)
    }

    fun clearDb() {
        use {
            dropTable(SeriesProgressScheme.TABLE, true)
            onCreate(this)
        }
    }

    companion object {
        private const val DB_NAME = "seriesProgress.db"
        private const val DB_VERSION = 1
    }
}

open class SeriesProgressDb(val dbHelper: SeriesProgressDbHelper) {
    // Pair<SeriesId, Progress>
    private val changeSubject = SerializedSubject(PublishSubject.create<Pair<Int, Int>>())

    /**
     * Set the progress for a specific series.
     * @param seriesId series to set progress for
     * @param progress progress of the series
     * @return an [Observable] emitting onError or onCompleted
     */
    open fun setProgress(seriesId: Int, progress: Int): Observable<Unit> {
        return dbHelper.useAsync {
            transaction {
                replace(SeriesProgressScheme.TABLE,
                        SeriesProgressScheme.ID to seriesId,
                        SeriesProgressScheme.LAST_EPISODE to progress)
            }
        }.doOnCompleted { changeSubject.onNext(seriesId to progress) }
    }

    /**
     * Get the progress for a series
     * @param seriesId series to get the progress for
     * @return an [Observable] emitting the progress
     */
    open fun getProgress(seriesId: Int): Observable<Int> {
        return dbHelper.useAsync {
            select(SeriesProgressScheme.TABLE, SeriesProgressScheme.LAST_EPISODE)
                    .where("(${SeriesProgressScheme.ID} = $seriesId)").parseOpt(IntParser)
        }.map { it ?: 0 }
    }

    /**
     * Observe the progress for a series
     * @param seriesId series to get the progress for
     * @return an [Observable] emitting the progress and any subsequent changes
     */
    fun observeProgress(seriesId: Int): Observable<Int> {
        return Observable.concat(
                getProgress(seriesId),
                changeSubject.filter { seriesId == it.first }.map { it.second })
    }
}