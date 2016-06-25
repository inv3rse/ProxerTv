package com.inverse.unofficial.proxertv.base

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

fun containsCaptcha(htmlBody: String): Boolean {
    return htmlBody.contains("<div id=\"captcha\"")
}

/**
 * Interceptor that makes sure that a captcha response is not loaded from cache.
 */
class NoCacheCaptchaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response? {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.url().toString().contains("proxer.me/watch") && response.cacheResponse() != null) {
            // We have to check the response body.
            // Because we can only read it once, the original response can not be returned
            val mediaType = response.body().contentType()
            val htmlBody = response.body().string()

            if (containsCaptcha(htmlBody)) {
                val noCacheRequest = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build()

                return chain.proceed(noCacheRequest)
            } else {
                val newBody = ResponseBody.create(mediaType, htmlBody)
                return response.newBuilder().body(newBody).build()
            }
        } else {
            return response
        }
    }

}