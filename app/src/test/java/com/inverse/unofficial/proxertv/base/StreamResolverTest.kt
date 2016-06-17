package com.inverse.unofficial.proxertv.base

import MockRedirector
import com.google.gson.Gson
import loadResponse
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import rx.observers.TestSubscriber

class StreamResolverTest {
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

    lateinit var httpClient: OkHttpClient

    @Before
    fun setup() {
        httpClient = OkHttpClient.Builder()
                .addInterceptor(MockRedirector(mockServer.url("").toString()))
                .build()
    }

    @Test
    fun testProxerStreamResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/proxerStreamResponse.html")))
        val resolver = ProxerStreamResolver(httpClient)

        assertTrue(resolver.appliesToUrl("http://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html"))
        assertTrue(resolver.appliesToUrl("https://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html"))

        val subscriber = TestSubscriber<String>()
        resolver.resolveStream("http://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html")
                .subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()

        assertEquals(1, subscriber.onNextEvents.size)
        assertEquals("http://s36.stream.proxer.me/files/7/wqj6extx5r6ztv/video.mp4", subscriber.onNextEvents[0])
    }

    @Test
    fun testStreamcloudResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/streamcloudResponse1.html")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/streamcloudResponse2.html")))
        val resolver = StreamCloudResolver(httpClient)

        assertTrue(resolver.appliesToUrl("http://streamcloud.eu/rma6ijnb58n0"))
        assertTrue(resolver.appliesToUrl("https://streamcloud.eu/rma6ijnb58n0"))

        val subscriber = TestSubscriber<String>()
        resolver.resolveStream("http://streamcloud.eu/rma6ijnb58n0")
                .subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()

        assertEquals(1, subscriber.onNextEvents.size)
        assertEquals("http://cdn8.streamcloud.eu:8080/zpv75jl4lwoax3ptx32ihr7fuses3udb2dpgvrl344izxpszn4p4ldp3nm/video.mp4", subscriber.onNextEvents[0])
    }

    @Test
    fun testDailyMotionResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/dailyMotionResponse.html")))
        val resolver = DailyMotionStreamResolver(httpClient, Gson())

        assertTrue(resolver.appliesToUrl("//www.dailymotion.com/embed/video/k12rpohEbcvbCfgIfPa"))
        assertTrue(resolver.appliesToUrl("www.dailymotion.com/embed/video/k12rpohEbcvbCfgIfPa"))
        assertTrue(resolver.appliesToUrl("https//www.dailymotion.com/embed/video/k12rpohEbcvbCfgIfPa"))

        val subscriber = TestSubscriber<String>()
        resolver.resolveStream("//www.dailymotion.com/embed/video/k12rpohEbcvbCfgIfPa")
                .subscribe(subscriber)

        subscriber.awaitTerminalEvent()
        subscriber.assertNoErrors()

        assertEquals(1, subscriber.onNextEvents.size)
        assertEquals("http://www.dailymotion.com/cdn/H264-1280x720/video/x431e80.mp4?auth=1465835647-2688-s9uh39e0-30c112e85600d8f9baaf5bcd11646584", subscriber.onNextEvents[0])
    }
}