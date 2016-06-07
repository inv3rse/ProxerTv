package com.example.dennis.proxertv.base

import com.example.dennis.proxertv.model.Series
import com.example.dennis.proxertv.model.SeriesCover
import com.example.dennis.proxertv.model.ServerConfig
import com.google.gson.Gson
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
                .readTimeout(180, TimeUnit.SECONDS).build()
        val resolvers = listOf(ProxerStreamResolver(httpClient), StreamCloudResolver(httpClient))

        proxerClient = ProxerClient(httpClient, Gson(), resolvers, ServerConfig(mockServer.url("/").toString()))
    }

    @Test
    fun testLoadTopAccessList() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("testTopAccessListResponse.html")))

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
    fun testLoadSeries() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("testDetailResponse.html")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("testDetailEpisodes.json")))

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
        assertEquals(8, series.availAbleEpisodes["gersub"]?.size)
        assertEquals(8, series.availAbleEpisodes["engsub"]?.size)
    }

    @Test
    fun testLoadEpisodeStreams() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("testWatchResponse.html")))
        // Todo redirect following resolve requests to offline data

        val subscriber = TestSubscriber<String>()
        proxerClient.loadEpisodeStreams(15371, 1, "engsub").subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()

        print(subscriber.onNextEvents)
    }
}