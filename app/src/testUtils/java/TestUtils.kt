import okhttp3.Interceptor
import okhttp3.Response
import okio.Okio
import rx.Observable
import rx.Subscription
import rx.observers.TestSubscriber
import java.nio.charset.Charset

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

fun <T> Observable<T>.subscribeAssert(assert: TestSubscriber<T>.() -> Unit): Subscription {
    val subscriber = TestSubscriber<T>()
    val subscription = subscribe(subscriber)
    subscriber.awaitTerminalEvent()
    subscriber.assert()
    return subscription
}