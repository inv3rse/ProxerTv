package com.inverse.unofficial.proxertv.base.client

import ApiResponses
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.SeriesUpdate
import loadResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import provideTestClient
import rx.observers.TestSubscriber
import subscribeAssert
import java.util.*

class ProxerClientTest {
    lateinit var mockServer: MockWebServer
    lateinit var proxerClient: ProxerClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        proxerClient = provideTestClient(mockServer)
    }

    @After
    fun after() {
        mockServer.shutdown()
    }

    @Test
    fun testLoadTopAccessList() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/topAccessListResponse.html")))

        val subscriber = TestSubscriber<List<SeriesCover>>()
        proxerClient.loadTopAccessSeries().subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)

        assertEquals(50, subscriber.onNextEvents[0].size)
        val kabaneri = SeriesCover(15371, "Koutetsujou no Kabaneri")
        assertEquals(kabaneri, subscriber.onNextEvents[0][0])
    }

    @Test
    fun testLoadUpdateList() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/updateListResponse.html")))

        val calendar = GregorianCalendar.getInstance()

        calendar.clear()
        calendar.set(2016, 6, 16, 0, 0, 0)
        val date16_7 = calendar.time
        calendar.set(2016, 6, 15, 0, 0, 0)
        val date15_7 = calendar.time
        calendar.set(2016, 6, 13, 0, 0, 0)
        val date13_7 = calendar.time
        calendar.set(2016, 5, 18, 0, 0, 0)
        val date18_6 = calendar.time

        val series1 = SeriesCover(14873, "Kyoukai no Rinne (TV) 2nd Season")
        val series2 = SeriesCover(16257, "91 Days")
        val series3 = SeriesCover(16330, "Cheer Danshi!!")
        val series4 = SeriesCover(14889, "Magi: Sinbad no Bouken")

        val wrongSeries1 = SeriesCover(14873, "Updates")
        val wrongSeries2 = SeriesCover(16257, "Updates")

        proxerClient.loadUpdatesList().subscribeAssert {
            assertNoErrors()
            assertValueCount(1)
            assertEquals(SeriesUpdate(series1, date16_7), onNextEvents[0][0])
            assertTrue(onNextEvents[0].contains(SeriesUpdate(series2, date15_7)))
            assertTrue(onNextEvents[0].contains(SeriesUpdate(series3, date13_7)))
            assertTrue(onNextEvents[0].contains(SeriesUpdate(series4, date18_6)))

            assertFalse(onNextEvents[0].contains(SeriesUpdate(wrongSeries1, date16_7)))
            assertFalse(onNextEvents[0].contains(SeriesUpdate(wrongSeries2, date15_7)))
        }
    }

    @Test
    fun testSearchSeries() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/searchResponse.html")))
        val subscriber = TestSubscriber<List<SeriesCover>>()

        proxerClient.searchSeries("Rakudai").subscribe(subscriber)
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)

        assertEquals(1, subscriber.onNextEvents[0].size)
        val rakudai = SeriesCover(12806, "Rakudai Kishi no Cavalry")
        assertEquals(rakudai, subscriber.onNextEvents[0][0])
    }

    @Test
    fun testLoadSeries() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/series_detail_15371.json")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/series_detail_46.json")))

        proxerClient.loadSeries(15371).subscribeAssert {
            assertNoErrors()
            assertValueCount(1)

            val series = onNextEvents[0]
            assertNotNull(series)
            assertEquals(15371, series.id)
            assertEquals("Koutetsujou no Kabaneri", series.name)
            assertEquals(12, series.count)
            assertEquals(1, series.pages())
        }

        proxerClient.loadSeries(46).subscribeAssert {
            assertNoErrors()
            assertValueCount(1)

            val series = onNextEvents[0]
            assertNotNull(series)
            assertEquals(46, series.id)
            assertEquals("Naruto Shippuuden", series.name)
            assertEquals(500, series.count)
            assertEquals(10, series.pages())
        }
    }

    @Test
    fun testLoadEpisodeStreams() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/seriesCaptchaResponse.html")))

        proxerClient.loadEpisodeStreams(13975, 3, "engsub").subscribeAssert {
            assertError(ProxerClient.SeriesCaptchaException::class.java)
            assertNoValues()
        }

//        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/watchResponse.html")))
//        // Todo redirect following resolve requests to offline data
//        proxerClient.loadEpisodeStreams(15371, 1, "engsub").subscribeAssert {
//            assertNoErrors()
//            print(onNextEvents)
//        }
    }

    @Test
    fun testApiErrorExceptionThrown() {
        mockServer.enqueue(ApiResponses.getErrorResponse(3003, "User existiert nicht"))
        proxerClient.userList().subscribeAssert {
            assertError(ApiErrorException::class.java)
            assertEquals(3003, (onErrorEvents[0] as ApiErrorException).code)
            assertEquals("User existiert nicht", (onErrorEvents[0] as ApiErrorException).msg)
        }

        mockServer.enqueue(ApiResponses.getErrorResponse(2000, "IP von Firewall geblockt."))
        proxerClient.loadSeries(1234).subscribeAssert {
            assertError(ApiErrorException::class.java)
            assertEquals(2000, (onErrorEvents[0] as ApiErrorException).code)
            assertEquals("IP von Firewall geblockt.", (onErrorEvents[0] as ApiErrorException).msg)
        }
    }
}