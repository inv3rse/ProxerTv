package com.inverse.unofficial.proxertv.db

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.RenamingDelegatingContext
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDbHelper
import com.inverse.unofficial.proxertv.base.db.SeriesProgressRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rx.observers.TestSubscriber
import subscribeAssert
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SeriesProgressRepositoryTest {

    lateinit var dbHelper: SeriesProgressDbHelper
    lateinit var repository: SeriesProgressRepository

    @Before
    fun setup() {
        @Suppress("DEPRECATION")
        val context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test")
        dbHelper = SeriesProgressDbHelper(context)
        repository = SeriesProgressRepository(dbHelper)

        dbHelper.clearDb()
    }

    @After
    fun tearDown() {
        dbHelper.clearDb()
    }

    @Test
    fun testSetSeriesProgress() {
        repository.setProgress(1337, 4).subscribeAssert { assertNoErrors() }
        repository.getProgress(1337).subscribeAssert { assertNoErrors(); assertValue(4) }

        repository.setProgress(15, 2).subscribeAssert { assertNoErrors() }
        repository.getProgress(15).subscribeAssert { assertNoErrors(); assertValue(2) }
        repository.getProgress(1337).subscribeAssert { assertNoErrors(); assertValue(4) }

        repository.setProgress(1337, 0).subscribeAssert { assertNoErrors() }
        repository.getProgress(1337).subscribeAssert { assertNoErrors(); assertValue(0) }

        repository.setProgress(-666, 266).subscribeAssert { assertNoErrors() }
        repository.getProgress(-666).subscribeAssert { assertNoErrors(); assertValue(266) }
    }

    @Test
    fun testObserveProgress() {
        val testSubscriberEmpty = TestSubscriber<Int>()
        val testSubscriberInitial = TestSubscriber<Int>()

        // no initial value set, should default to 0
        repository.observeProgress(42).take(4).subscribe(testSubscriberEmpty)

        repository.setProgress(42, 4).subscribeAssert { assertNoErrors() }
        repository.setProgress(42, 3).subscribeAssert { assertNoErrors() }
        repository.setProgress(42, 8).subscribeAssert { assertNoErrors() }

        testSubscriberEmpty.awaitTerminalEvent(5, TimeUnit.SECONDS)
        testSubscriberEmpty.assertNoErrors()
        testSubscriberEmpty.assertValues(0, 4, 3, 8)

        // initial value should be 8
        repository.observeProgress(42).take(3).subscribe(testSubscriberInitial)

        repository.setProgress(42, 4).subscribeAssert { assertNoErrors() }
        repository.setProgress(42, 5).subscribeAssert { assertNoErrors() }

        testSubscriberInitial.awaitTerminalEvent(5, TimeUnit.SECONDS)
        testSubscriberInitial.assertNoErrors()
        testSubscriberInitial.assertValues(8, 4, 5)
    }
}