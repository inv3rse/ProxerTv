package com.inverse.unofficial.proxertv.base

import com.google.gson.Gson
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.ServerConfig
import loadResponse
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import rx.observers.TestSubscriber
import subscribeAssert
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