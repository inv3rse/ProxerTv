package com.inverse.unofficial.proxertv.base.client.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the api key header to every request.
 */
class ApiKeyInterceptor(val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request.newBuilder().header(KEY_HEADER, apiKey).build())
    }

    companion object {
        private const val KEY_HEADER = "proxer-api-key"
    }
}