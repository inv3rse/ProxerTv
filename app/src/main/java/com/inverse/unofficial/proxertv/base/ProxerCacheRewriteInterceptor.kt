package com.inverse.unofficial.proxertv.base

import okhttp3.Interceptor
import okhttp3.Response

/**
 * An interceptor that overrides the cache header
 */
class ProxerCacheRewriteInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response? {

        val request = chain.request()
        val response = chain.proceed(request)

        if (request.url().host().contains("proxer.me")) {
            return response.newBuilder()
                    .header("Cache-Control", "max-age=1800")
                    .removeHeader("Pragma")
                    .build()
        } else {
            return response
        }
    }
}