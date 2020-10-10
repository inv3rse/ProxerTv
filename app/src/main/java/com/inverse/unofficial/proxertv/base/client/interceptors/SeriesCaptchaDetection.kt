package com.inverse.unofficial.proxertv.base.client.interceptors

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
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val responseBody = response.body
        if (request.url.toString().contains("proxer.me/watch")
                && response.cacheResponse != null
                && responseBody != null) {

            // We have to check the response body.
            // Because we can only read it once, the original response can not be returned
            val mediaType = responseBody.contentType()
            val htmlBody = responseBody.string()

            return if (containsCaptcha(htmlBody)) {
                val noCacheRequest = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build()

                chain.proceed(noCacheRequest)
            } else {
                val newBody = ResponseBody.create(mediaType, htmlBody)
                response.newBuilder().body(newBody).build()
            }
        } else {
            return response
        }
    }

}
