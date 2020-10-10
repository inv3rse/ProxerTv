package com.inverse.unofficial.proxertv.base.client.interceptors

import com.inverse.unofficial.proxertv.model.ServerConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An interceptor that overrides the cache header
 */
class ProxerCacheRewriteInterceptor(private val serverConfig: ServerConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val response = chain.proceed(request)

        if (request.url.host.contains(serverConfig.host)
                && request.method.equals("GET", true)
                && response.isSuccessful) {

            val maxAge = when (request.url.toString()) {
                serverConfig.topAccessListUrl(),
                serverConfig.topRatingListUrl(),
                serverConfig.topRatingMovieListUrl(),
                serverConfig.airingListUrl() -> 14400 // 4h
                else -> 1800 // 30 min
            }

            return response.newBuilder()
                    .header("Cache-Control", "max-age=$maxAge")
                    .removeHeader("Pragma")
                    .build()
        } else {
            return response
        }
    }
}
