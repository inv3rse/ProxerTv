package com.example.dennis.proxertv.base

import android.util.Log
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
            val builder = response.newBuilder()
                    .header("Cache-Control", "max-age=1800, max-stale=600")
                    .removeHeader("Pragma")

            val lastModified = response.header("Last-Modified")
            if (lastModified != null) {
                builder.header("Expires", lastModified)
            }

            return builder.build()
        } else {
            return response
        }
    }
}