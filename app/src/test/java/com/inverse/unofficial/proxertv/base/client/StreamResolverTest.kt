package com.inverse.unofficial.proxertv.base.client

import MockRedirector
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.base.client.util.*
import com.inverse.unofficial.proxertv.model.Stream
import loadResponse
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import subscribeAssert

class StreamResolverTest {
    private lateinit var httpClient: OkHttpClient
    private val mockServer = MockWebServer()

    @Before
    fun setup() {
        mockServer.start()

        httpClient = OkHttpClient.Builder()
                .addInterceptor(MockRedirector(mockServer.url("").toString()))
                .build()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun testProxerStreamResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/proxerStreamResponse.html")))
        val resolver = ProxerStreamResolver(httpClient)

        assertTrue(resolver.appliesToUrl("http://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html"))
        assertTrue(resolver.appliesToUrl("https://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html"))

        resolver.resolveStream("http://stream.proxer.me/embed-zbtli7pqg8mg-728x504.html")
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(Stream("http://s36.stream.proxer.me/files/7/wqj6extx5r6ztv/video.mp4", ""))
                }
    }

    @Test
    fun testStreamcloudResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/streamcloudResponse1.html")))
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/streamcloudResponse2.html")))
        val resolver = StreamCloudResolver(httpClient)

        assertTrue(resolver.appliesToUrl("http://streamcloud.eu/rma6ijnb58n0"))
        assertTrue(resolver.appliesToUrl("https://streamcloud.eu/rma6ijnb58n0"))

        resolver.resolveStream("http://streamcloud.eu/rma6ijnb58n0")
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(Stream("http://cdn8.streamcloud.eu:8080/zpv75jl4lwoax3ptx32ihr7fuses3udb2dpgvrl344izxpszn4p4ldp3nm/video.mp4", ""))
                }
    }

    @Test
    fun testMp4UploadStreamResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/mp4uploadResponse.html")))
        val resolver = Mp4UploadStreamResolver(httpClient)

        assertTrue(resolver.appliesToUrl("http://www.mp4upload.com/embed-sg71lpccevaa.html"))
        assertTrue(resolver.appliesToUrl("https://www.mp4upload.com/embed-sg71lpccevaa.html"))

        resolver.resolveStream("http://www.mp4upload.com/embed-sg71lpccevaa.html").subscribeAssert {
            assertNoErrors()
            assertValue(Stream("https://www2.mp4upload.com:282/d/rgxzcgtmz3b4quuofwvbwksoi244w36p4ot4zdpu3sak6gbf5myfbrny/video.mp4", ""))
        }
    }

    @Test
    fun testDailyMotionResolver() {
        mockServer.enqueue(MockResponse().setBody(loadResponse("StreamResolverTest/dailyMotionResponse.html")))
        val resolver = DailyMotionStreamResolver(httpClient, Gson())

        assertTrue((resolver.appliesToUrl("http://www.dailymotion.com/embed/video/x5tqens")))
        assertTrue(resolver.appliesToUrl("https://www.dailymotion.com/embed/video/x5tqens"))

        resolver.resolveStream("http://www.dailymotion.com/embed/video/x5tqens")
                .subscribeAssert {
                    assertNoErrors()
                    assertValues(
                            Stream("http://www.dailymotion.com/cdn/H264-1280x720/video/x5tqens.mp4?auth=1501533278-2688-ysrzfgvg-81963c69aacf70e54cc2fa56f209af40", ""),
                            Stream("http://www.dailymotion.com/cdn/H264-848x480/video/x5tqens.mp4?auth=1501533278-2688-r8o5lwqa-ab206b484b0ab0de4c77255027008251", ""))
                }
    }

    private fun StreamResolver.appliesToUrl(url: String) = appliesToUrl(HttpUrl.parse(url))

    private fun StreamResolver.resolveStream(url: String) = resolveStream(HttpUrl.parse(url))
}