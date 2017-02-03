package com.inverse.unofficial.proxertv.db

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.RenamingDelegatingContext
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesDbHelper
import com.inverse.unofficial.proxertv.model.ISeriesDbEntry
import com.inverse.unofficial.proxertv.model.SeriesDbEntry
import com.inverse.unofficial.proxertv.model.SeriesList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rx.observers.TestSubscriber
import subscribeAssert
import java.util.concurrent.TimeUnit

/**
 * Test cases for the [MySeriesDb]
 */
@RunWith(AndroidJUnit4::class)
class MySeriesDbTest {

    lateinit var dbHelper: SeriesDbHelper
    lateinit var seriesDb: MySeriesDb

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        val context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test")
        dbHelper = SeriesDbHelper(context)
        seriesDb = MySeriesDb(dbHelper)

        dbHelper.clearDb()
    }

    @After
    fun tearDown() {
        dbHelper.clearDb()
    }

    @Test
    fun testAddSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.FINISHED)
        val series3 = SeriesDbEntry(3, "3", SeriesList.NONE)

        seriesDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        seriesDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add same key (should not appear twice)
        seriesDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        seriesDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add series 2
        seriesDb.insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
        seriesDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1, series2))
        }

        // adding to list NONE should do nothing
        seriesDb.insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        seriesDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1, series2))
        }
    }

    @Test
    fun testContainsSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.FINISHED)
        val series3 = SeriesDbEntry(3, "3", SeriesList.ABORTED)

        seriesDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        }

        seriesDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        seriesDb.containsSeries(series2.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        seriesDb.containsSeries(series3.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        seriesDb.containsSeries(4).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }
    }

    @Test
    fun testGetSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.FINISHED)

        seriesDb.getSeries(1).subscribeAssert { assertError(MySeriesDb.NoSeriesEntryException::class.java) }

        seriesDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        seriesDb.getSeries(1).subscribeAssert {
            assertNoErrors()
            assertValue(series1)
        }

        seriesDb.insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }

        seriesDb.getSeries(1).subscribeAssert {
            assertNoErrors()
            assertValue(series1)
        }

        seriesDb.getSeries(2).subscribeAssert {
            assertNoErrors()
            assertValue(series2)
        }
    }

    @Test
    fun testRemoveSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.ABORTED)
        val series2 = SeriesDbEntry(2, "2", SeriesList.WATCHLIST)
        val series3 = SeriesDbEntry(3, "3", SeriesList.WATCHLIST)

        seriesDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        }

        seriesDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        seriesDb.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        seriesDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }

        seriesDb.containsSeries(series3.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }
    }

    @Test
    fun testObserveSeriesList() {
        val testSubscriber = TestSubscriber<List<ISeriesDbEntry>>()
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.WATCHLIST)
        val series3 = SeriesDbEntry(3, "3", SeriesList.ABORTED)

        seriesDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
        }

        seriesDb.observeSeriesList().take(3).subscribe(testSubscriber)

        seriesDb.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        seriesDb.insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }

        testSubscriber.awaitTerminalEvent(1, TimeUnit.SECONDS)
        testSubscriber.assertNoErrors()
        // initial, remove1, add3
        testSubscriber.assertValues(listOf(series1, series2), listOf(series2), listOf(series2, series3))
    }

    @Test
    fun testObserveSeriesListState() {
        val testSubscriber = TestSubscriber<SeriesList>()
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.WATCHLIST)

        seriesDb.observeSeriesListState(series1.id).subscribe(testSubscriber)   // initial value

        // to SeriesList.WATCHLIST
        seriesDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        // no change
        seriesDb.insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
        // to SeriesList.FINISHED
        seriesDb.insertOrUpdateSeries(series1.copy(userList = SeriesList.FINISHED)).subscribeAssert { assertNoErrors() }
        // to SeriesList.NONE
        seriesDb.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        // to SeriesList.WATCHLIST
        seriesDb.overrideWithSeriesList(listOf(series1, series2)).subscribeAssert { assertNoErrors() }

        testSubscriber.awaitTerminalEvent(1, TimeUnit.SECONDS)
        testSubscriber.assertNoErrors()
        testSubscriber.assertValues(SeriesList.NONE, SeriesList.WATCHLIST, SeriesList.FINISHED, SeriesList.NONE, SeriesList.WATCHLIST)
    }
}