import com.google.gson.Gson
import com.inverse.unofficial.proxertv.base.client.ProxerApi
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiResponseConverterFactory
import com.inverse.unofficial.proxertv.base.client.util.ProxerStreamResolver
import com.inverse.unofficial.proxertv.base.client.util.StreamCloudResolver
import com.inverse.unofficial.proxertv.model.ServerConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Okio
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable
import rx.Subscription
import rx.observers.TestSubscriber
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun loadResponse(file: String): String {
    val inputStream = ClassLoader.getSystemResourceAsStream(file)
    return Okio.buffer(Okio.source(inputStream)).readString(Charset.defaultCharset())
}

class MockRedirector(val targetHost: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val redirected = request.newBuilder().url(targetHost + request.url().encodedPath()).build()
        return chain.proceed(redirected)
    }
}

fun provideTestClient(mockWebServer: MockWebServer): ProxerClient {
    val httpClient = OkHttpClient.Builder().connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()
    val resolvers = listOf(ProxerStreamResolver(httpClient), StreamCloudResolver(httpClient))
    val mockServerUrl = mockWebServer.url("/")
    val serverConfig = ServerConfig(mockServerUrl.scheme(), mockServerUrl.host() + ":" + mockServerUrl.port())
    val gson = Gson()
    val api = Retrofit.Builder()
            .baseUrl(serverConfig.apiBaseUrl)
            .client(httpClient)
            .addConverterFactory(ApiResponseConverterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build().create(ProxerApi::class.java)

    return ProxerClient(httpClient, api, gson, resolvers, serverConfig)
}

fun <T> Observable<T>.subscribeAssert(assert: TestSubscriber<T>.() -> Unit): Subscription {
    val subscriber = TestSubscriber<T>()
    val subscription = subscribe(subscriber)
    subscriber.awaitTerminalEvent()
    subscriber.assert()
    return subscription
}

/**
 * Provides mock responses for the [ProxerApi]
 */
object ApiResponses {

    /**
     * Get a successful login response
     * @param token the response token
     * @return the response
     */
    fun getSuccessFulLoginResponse(token: String): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 0," +
                "\"message\": \"Login erfolgreich\"," +
                "\"data\": {" +
                "\"uid\": \"12345\"," +
                "\"avatar\": \"122345.jpg\"," +
                "\"token\": \"" + token + "\"" +
                "}}")
    }

    /**
     * Get a successful logout response
     * @return the response
     */
    fun getLogoutResponse(): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 0," +
                "\"message\": \"Logout successfull\"" +
                "}")
    }

    /**
     * Get a error response
     * @param errorCode the error code
     * @param msg the error message
     * @return the response
     */
    fun getErrorResponse(errorCode: Int, msg: String): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 1," +
                "\"message\": \"" + msg + "\"," +
                "\"code\": " + errorCode +
                "}")
    }
}