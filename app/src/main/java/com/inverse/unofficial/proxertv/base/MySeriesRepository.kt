package com.inverse.unofficial.proxertv.base

import com.inverse.unofficial.proxertv.model.SeriesCover
import org.jetbrains.anko.db.*
import rx.Observable
import rx.subjects.BehaviorSubject

class MySeriesRepository(val dbHelper: SeriesDbHelper) {

    private val listObservable = BehaviorSubject.create<List<SeriesCover>>()
    private var firstLoaded = false

    fun observeSeriesList(): Observable<List<SeriesCover>> {
        if (!firstLoaded) {
            notifyListChange()
            firstLoaded = true
        }

        return listObservable
    }

    fun loadSeriesList(): Observable<List<SeriesCover>> {
        return Observable.fromCallable {
            internalLoadSeriesList()
        }
    }

    fun addSeries(series: SeriesCover) {
        dbHelper.use {
            transaction {
                insert(SeriesScheme.TABLE,
                        SeriesScheme.ID to series.id,
                        SeriesScheme.TITLE to series.title,
                        SeriesScheme.IMAGE to series.coverImage)
            }
        }

        notifyListChange()
    }

    fun containsSeries(seriesId: Int): Observable<Boolean> {
        return Observable.fromCallable {
            dbHelper.use {
                select(SeriesScheme.TABLE).where("(${SeriesScheme.ID} = $seriesId)").exec { count > 0 }
            }
        }
    }

    fun removeSeries(seriesId: Int) {
        dbHelper.use {
            transaction {
                delete(SeriesScheme.TABLE, "(${SeriesScheme.ID} = $seriesId)")
            }
        }

        notifyListChange()
    }

    private fun notifyListChange() {
        if (listObservable.hasObservers() || !firstLoaded) {
            // should probably be async, but the order should not change
            val seriesList = internalLoadSeriesList()
            listObservable.onNext(seriesList)
        }
    }

    private fun internalLoadSeriesList(): List<SeriesCover> {
        return dbHelper.use { select(SeriesScheme.TABLE).exec { parseList(SeriesRowParser()) } }
    }

    private class SeriesRowParser : RowParser<SeriesCover> {
        private val parser = rowParser { id: Int, title: String, image: String -> SeriesCover(id, title, image) }

        override fun parseRow(columns: Array<Any>): SeriesCover {
            return parser.parseRow(columns)
        }
    }
}