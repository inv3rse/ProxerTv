package com.inverse.unofficial.proxertv.db

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.RenamingDelegatingContext
import com.inverse.unofficial.proxertv.base.db.MySeriesRepository
import com.inverse.unofficial.proxertv.base.db.SeriesDbHelper
import com.inverse.unofficial.proxertv.model.SeriesCover
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rx.observers.TestSubscriber
import subscribeAssert
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MySeriesRepositoryTest {

    lateinit var dbHelper: SeriesDbHelper
    lateinit var repository: MySeriesRepository

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        val context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test")
        dbHelper = SeriesDbHelper(context)
        repository = MySeriesRepository(dbHelper)

        dbHelper.clearDb()
    }

    @After
    fun tearDown() {
        dbHelper.clearDb()
    }

    @Test
    fun testAddSeries() {
        val series1 = SeriesCover(1, "1")
        val series2 = SeriesCover(2, "2")

        repository.addSeries(series1).subscribeAssert { assertNoErrors() }
        repository.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add same key (should not appear twice)
        repository.addSeries(series1).subscribeAssert { assertNoErrors() }
        repository.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1))
        }

        // add series 2
        repository.addSeries(series2).subscribeAssert { assertNoErrors() }
        repository.loadSeriesList().subscribeAssert {
            assertNoErrors()
            assertValue(listOf(series1, series2))
        }
    }

    @Test
    fun testContainsSeries() {
        val series1 = SeriesCover(1, "1")
        val series2 = SeriesCover(2, "2")
        val series3 = SeriesCover(3, "3")

        repository.apply {
            addSeries(series1).subscribeAssert { assertNoErrors() }
            addSeries(series2).subscribeAssert { assertNoErrors() }
            addSeries(series3).subscribeAssert { assertNoErrors() }
        }

        repository.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        repository.containsSeries(series2.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        repository.containsSeries(series3.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        repository.containsSeries(4).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }
    }

    @Test
    fun testRemoveSeries() {
        val series1 = SeriesCover(1, "1")
        val series2 = SeriesCover(2, "2")
        val series3 = SeriesCover(3, "3")

        repository.apply {
            addSeries(series1).subscribeAssert { assertNoErrors() }
            addSeries(series2).subscribeAssert { assertNoErrors() }
            addSeries(series3).subscribeAssert { assertNoErrors() }
        }

        repository.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }

        repository.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        repository.containsSeries(series1.id).subscribeAssert {
            assertNoErrors()
            assertValue(false)
        }

        repository.containsSeries(series3.id).subscribeAssert {
            assertNoErrors()
            assertValue(true)
        }
    }

    @Test
    fun testObserveSeriesList() {
        val testSubscriber = TestSubscriber<List<SeriesCover>>()
        val series1 = SeriesCover(1, "1")
        val series2 = SeriesCover(2, "2")
        val series3 = SeriesCover(3, "3")

        repository.apply {
            addSeries(series1).subscribeAssert { assertNoErrors() }
            addSeries(series2).subscribeAssert { assertNoErrors() }
        }

        repository.observeSeriesList().take(3).subscribe(testSubscriber)

        repository.removeSeries(series1.id).subscribeAssert { assertNoErrors() }
        repository.addSeries(series3).subscribeAssert { assertNoErrors() }

        testSubscriber.awaitTerminalEvent(1, TimeUnit.SECONDS)
        testSubscriber.assertNoErrors()
        // initial, remove1, add3
        testSubscriber.assertValues(listOf(series1, series2), listOf(series2), listOf(series2, series3))
    }
}