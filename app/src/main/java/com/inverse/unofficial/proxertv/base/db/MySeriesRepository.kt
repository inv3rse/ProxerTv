package com.inverse.unofficial.proxertv.base.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.inverse.unofficial.proxertv.model.SeriesCover
import org.jetbrains.anko.db.*
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.SerializedSubject

internal object SeriesScheme {
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

class MySeriesRepository(val dbHelper: SeriesDbHelper) {
    private val listObservable = SerializedSubject(BehaviorSubject.create<Unit>(Unit))

    /**
     * Observe the series list.
     * @return an Observable emitting the current value and any subsequent changes
     */
    fun observeSeriesList(): Observable<List<SeriesCover>> {
        return listObservable.concatMap { loadSeriesList() }
    }

    /**
     * Loads the series list.
     * @return an Observable emitting the current value
     */
    fun loadSeriesList(): Observable<List<SeriesCover>> {
        return dbHelper.useAsync {
            select(SeriesScheme.TABLE).parseList(SeriesRowParser())
        }
    }

    /**
     * Add a series to the list.
     * @param series series to add to the list
     * @return an Observable emitting onError or OnCompleted
     */
    fun addSeries(series: SeriesCover): Observable<Unit> {
        return dbHelper.useAsync {
            transaction {
                insert(SeriesScheme.TABLE,
                        SeriesScheme.ID to series.id,
                        SeriesScheme.TITLE to series.title,
                        SeriesScheme.IMAGE to series.coverImage)
            }
        }.doOnCompleted { notifyListChange() }
    }

    /**
     * Check if the list contains a specific series
     * @param seriesId id of the series to check for
     * @return an Observable emitting true or false
     */
    fun containsSeries(seriesId: Int): Observable<Boolean> {
        return dbHelper.useAsync {
            select(SeriesScheme.TABLE).where("(${SeriesScheme.ID} = $seriesId)").exec { count > 0 }
        }
    }

    /**
     * Remove a series from the list.
     * @param seriesId id of the series to remove
     * @return an Observable emitting onError or OnCompleted
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

    private class SeriesRowParser : RowParser<SeriesCover> {
        private val parser = rowParser { id: Int, title: String, image: String -> SeriesCover(id, title, image) }

        override fun parseRow(columns: Array<Any>): SeriesCover {
            return parser.parseRow(columns)
        }
    }
}