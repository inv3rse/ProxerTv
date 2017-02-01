package com.inverse.unofficial.proxertv.base.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.inverse.unofficial.proxertv.model.ISeriesDbEntry
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.SeriesDbEntry
import com.inverse.unofficial.proxertv.model.SeriesList
import org.jetbrains.anko.db.*
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.SerializedSubject

internal object SeriesScheme {
    const val TABLE = "mySeries"
    const val ID = "id"
    const val NAME = "name"
    const val COMMENT_ID = "commentId"
    const val USER_LIST = "userList"
}

/**
 * Helper for creating and updating the series db.
 */
class SeriesDbHelper(context: Context) : ManagedSQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(SeriesScheme.TABLE, true,
                SeriesScheme.ID to INTEGER + PRIMARY_KEY + UNIQUE,
                SeriesScheme.NAME to TEXT,
                SeriesScheme.COMMENT_ID to INTEGER,
                SeriesScheme.USER_LIST to INTEGER)
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
        private const val DB_VERSION = 4
    }
}

/**
 * The db for the users series.
 */
open class MySeriesDb(val dbHelper: SeriesDbHelper) {
    private val listObservable = SerializedSubject(BehaviorSubject.create<Unit>(Unit))

    /**
     * Observe the series list.
     * @return an [Observable] emitting the current value and any subsequent changes
     */
    fun observeSeriesList(): Observable<List<ISeriesDbEntry>> {
        return listObservable.concatMap { loadSeriesList() }
    }

    /**
     * Loads the series list.
     * @return an [Observable] emitting the current value
     */
    fun loadSeriesList(): Observable<List<ISeriesDbEntry>> {
        return dbHelper.useAsync {
            select(SeriesScheme.TABLE).parseList(SeriesRowParser())
        }
    }

    /**
     * Set the list of series. Deletes the current values and inserts the new ones.
     * @return an [Observable] emitting onError or OnCompleted
     */
    fun overrideWithSeriesList(seriesList: List<ISeriesDbEntry>): Observable<Unit> {
        return dbHelper.useAsync {
            transaction {
                delete(SeriesScheme.TABLE)
                seriesList
                        .filter { it.userList != SeriesList.NONE }
                        .forEach {
                            insert(SeriesScheme.TABLE,
                                    SeriesScheme.ID to it.id,
                                    SeriesScheme.NAME to it.name,
                                    SeriesScheme.COMMENT_ID to it.cid,
                                    SeriesScheme.USER_LIST to it.userList.ordinal)
                        }
            }
        }.doOnCompleted { notifyListChange() }
    }

    /**
     * Add a series to the list or replaces an existing one if the id already exists.
     * @param series series to add to the list
     * @return an [Observable] emitting onError or OnCompleted
     */
    fun insertOrUpdateSeries(series: ISeriesDbEntry): Observable<Unit> {
        if (series.userList == SeriesList.NONE) {
            return removeSeries(series.id)
        }

        return dbHelper.useAsync {
            transaction {
                replace(SeriesScheme.TABLE,
                        SeriesScheme.ID to series.id,
                        SeriesScheme.NAME to series.name,
                        SeriesScheme.COMMENT_ID to series.cid,
                        SeriesScheme.USER_LIST to series.userList.ordinal)
            }
        }.doOnCompleted { notifyListChange() }
    }

    /**
     * Get the series for a given id if it exists.
     * @param seriesId the series id
     * @return an [Observable] emitting the [SeriesCover] or throwing an error
     */
    fun getSeries(seriesId: Int): Observable<ISeriesDbEntry> {
        return dbHelper.useAsync {
            select(SeriesScheme.TABLE).where("(${SeriesScheme.ID} ) $seriesId)").exec {
                if (count == 1) {
                    parseSingle(SeriesRowParser())
                } else {
                    throw SQLiteException("no series for id \"$seriesId\"")
                }
            }
        }
    }

    /**
     * Check if the list contains a specific series
     * @param seriesId id of the series to check for
     * @return an [Observable] emitting true or false
     */
    fun containsSeries(seriesId: Int): Observable<Boolean> {
        return dbHelper.useAsync {
            select(SeriesScheme.TABLE).where("(${SeriesScheme.ID} = $seriesId)").exec { count > 0 }
        }
    }

    /**
     * Remove a series from the list.
     * @param seriesId id of the series to remove
     * @return an [Observable] emitting onError or OnCompleted
     */
    fun removeSeries(seriesId: Int): Observable<Unit> {
        return dbHelper.useAsync {
            transaction {
                delete(SeriesScheme.TABLE, "(${SeriesScheme.ID} = $seriesId)")
            }
        }.doOnCompleted { notifyListChange() }
    }

    private fun notifyListChange() {
        listObservable.onNext(Unit)
    }

    private class SeriesRowParser : RowParser<ISeriesDbEntry> {
        @Suppress("ConvertLambdaToReference")
        private val parser = rowParser { id: Int, title: String, cid: Long, list: Int -> SeriesDbEntry(id, title, SeriesList.fromOrdinal(list), cid) }

        override fun parseRow(columns: Array<Any?>): ISeriesDbEntry {
            return parser.parseRow(columns)
        }
    }
}