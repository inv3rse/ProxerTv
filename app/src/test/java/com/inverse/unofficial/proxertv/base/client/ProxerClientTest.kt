package com.inverse.unofficial.proxertv.base.client

import com.google.gson.Gson
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.SeriesUpdate
import com.inverse.unofficial.proxertv.model.ServerConfig
import loadResponse
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import rx.observers.TestSubscriber
import subscribeAssert
import java.util.*
import java.util.concurrent.TimeUnit

class ProxerClientTest {
    companion object {
        val mockServer = MockWebServer()

        @BeforeClass
        fun startWebServer() {
            mockServer.start()
        }

        @AfterClass
        fun stopWebServer() {
            mockServer.shutdown()
        }
    }

    lateinit var proxerClient: ProxerClient

    @Before
    fun setup() {
        val httpClient = OkHttpClient.Builder().connectTimeout(180, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build()
        val resolvers = listOf(ProxerStreamResolver(httpClient), StreamCloudResolver(httpClient))
        val mockServerUrl = mockServer.url("/")
        val serverConfig = ServerConfig(mockServerUrl.scheme(), mockServerUrl.host() + ":" + mockServerUrl.port())

        proxerClient = ProxerClient(httpClient, Gson(), resolvers, serverConfig)
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
        val kabaneri = SeriesCover(15371, "Koutetsujou no Kabaneri", "https://cdn.proxer.me/cover/15371.jpg")
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

        val series1 = SeriesCover(14873, "Kyoukai no Rinne (TV) 2nd Season", "https://cdn.proxer.me/cover/14873.jpg")
        val series2 = SeriesCover(16257, "91 Days", "https://cdn.proxer.me/cover/16257.jpg")
        val series3 = SeriesCover(16330, "Cheer Danshi!!", "https://cdn.proxer.me/cover/16330.jpg")
        val series4 = SeriesCover(14889, "Magi: Sinbad no Bouken", "https://cdn.proxer.me/cover/14889.jpg")

        val wrongSeries1 = SeriesCover(14873, "Updates", "https://cdn.proxer.me/cover/14873.jpg")
        val wrongSeries2 = SeriesCover(16257, "Updates", "https://cdn.proxer.me/cover/16257.jpg")

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
        val rakudai = SeriesCover(12806, "Rakudai Kishi no Cavalry", "https://cdn.proxer.me/cover/12806.jpg")
        assertEquals(rakudai, subscriber.onNextEvents[0][0])
    }

    @Test
    fun testLoadSeries() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/detailResponse.html")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/detailEpisodesSingle.html")))

        val subscriber = TestSubscriber<Series?>()
        proxerClient.loadSeries(15371).subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)

        val series = subscriber.onNextEvents[0]

        assertNotNull(series)
        assertEquals(15371, series!!.id)
        assertEquals("Koutetsujou no Kabaneri", series.originalTitle)
        assertEquals("Kabaneri of the Iron Fortress", series.englishTitle)
        assertEquals(1, series.pages)
    }

    @Test
    fun testLoadNumStreamSinglePage() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/detailEpisodesSingle.html")))
        val subscriber = TestSubscriber<Int>()

        // single page
        proxerClient.loadNumStreamPages(15371).subscribe(subscriber)
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)
        assertEquals(1, subscriber.onNextEvents[0])
    }

    @Test
    fun testLoadNumStreamMultiplePages() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("ProxerClientTest/detailEpisodesMultiple.html")))
        val subscriber = TestSubscriber<Int>()

        // multiple pages
        proxerClient.loadNumStreamPages(53).subscribe(subscriber)
        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()
        subscriber.assertValueCount(1)
        assertEquals(16, subscriber.onNextEvents[0])
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
}