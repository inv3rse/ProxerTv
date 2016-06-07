package com.example.dennis.proxertv.base

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
        mockServer.enqueue(MockResponse().setBody(loadResponse("testProxerStreamResponse.html")))
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
        mockServer.enqueue(MockResponse().setBody(loadResponse("testStreamcloudResponse1.html")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("testStreamcloudResponse2.html")))
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
}