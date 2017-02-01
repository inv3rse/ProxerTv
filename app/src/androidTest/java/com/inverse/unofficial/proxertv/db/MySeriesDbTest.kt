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
    lateinit var mDb: MySeriesDb

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        val context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test")
        dbHelper = SeriesDbHelper(context)
        mDb = MySeriesDb(dbHelper)

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

        mDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        mDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add same key (should not appear twice)
        mDb.insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
        mDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add series 2
        mDb.insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
        mDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1, series2))
        }

        // adding to list NONE should do nothing
        mDb.insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        mDb.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1, series2))
        }
    }

    @Test
    fun testContainsSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.WATCHLIST)
        val series2 = SeriesDbEntry(2, "2", SeriesList.FINISHED)
        val series3 = SeriesDbEntry(3, "3", SeriesList.ABORTED)

        mDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        }

        mDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        mDb.containsSeries(series2.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        mDb.containsSeries(series3.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        mDb.containsSeries(4).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }
    }

    @Test
    fun testRemoveSeries() {
        val series1 = SeriesDbEntry(1, "1", SeriesList.ABORTED)
        val series2 = SeriesDbEntry(2, "2", SeriesList.WATCHLIST)
        val series3 = SeriesDbEntry(3, "3", SeriesList.WATCHLIST)

        mDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }
        }

        mDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        mDb.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        mDb.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }

        mDb.containsSeries(series3.id).subscribeAssert {
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

        mDb.apply {
            insertOrUpdateSeries(series1).subscribeAssert { assertNoErrors() }
            insertOrUpdateSeries(series2).subscribeAssert { assertNoErrors() }
        }

        mDb.observeSeriesList().take(3).subscribe(testSubscriber)

        mDb.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        mDb.insertOrUpdateSeries(series3).subscribeAssert { assertNoErrors() }

        testSubscriber.awaitTerminalEvent(1, TimeUnit.SECONDS)
        testSubscriber.assertNoErrors()
        // initial, remove1, add3
        testSubscriber.assertValues(listOf(series1, series2), listOf(series2), listOf(series2, series3))
    }
}