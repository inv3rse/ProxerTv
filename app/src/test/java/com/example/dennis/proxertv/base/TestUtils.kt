package com.example.dennis.proxertv.base

import okhttp3.Interceptor
import okhttp3.Response
import okio.Okio
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