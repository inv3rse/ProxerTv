package com.inverse.unofficial.proxertv.base.client.interceptors

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * Interceptor that adds the api key to every request.
 * If the request is not of type POST already, we transform it to one.
 */
class ApiKeyInterceptor(val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val existingBody = if (request.body() != null && request.body().contentType() != null)
            request.body() else
            RequestBody.create(CONTENT_TYPE, "")

        val newBody = ApiRequestBody(existingBody, apiKey)
        return chain.proceed(request.newBuilder().post(newBody).build())
    }

    /**
     * Helper class that prepends our api key to the actual body
     */
    private class ApiRequestBody(val body: RequestBody, apiKey: String) : RequestBody() {
        private val apiString: String = "api_key=$apiKey&"

        override fun contentType(): MediaType {
            return body.contentType()
        }

        override fun writeTo(sink: BufferedSink) {
            sink.writeString(apiString, Charsets.UTF_8)
            body.writeTo(sink)
        }

        override fun contentLength(): Long {
            return if (body.contentLength() == -1L) -1L else body.contentLength() + apiString.length
        }
    }

    companion object {
        private val CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded")
    }
}