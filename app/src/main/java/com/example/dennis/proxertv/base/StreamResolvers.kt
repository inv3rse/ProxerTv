package com.example.dennis.proxertv.base

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

interface StreamResolver {
    /**
     * return true if the url might be resolvable
     */
    fun appliesToUrl(url: String): Boolean

    /**
     * try to find the stream url
     */
    fun resolveStream(url: String): Observable<String>
}

class ProxerStreamResolver(val httpClient: OkHttpClient) : StreamResolver {
    override fun appliesToUrl(url: String): Boolean {
        return url.contains("stream.proxer.me", true)
    }

    override fun resolveStream(url: String): Observable<String> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<String> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val regex = Regex("src=\"(.*\\.mp4)\"")
                    val streamUrl = regex.find(content)?.groupValues?.get(1)

                    if (streamUrl != null) {
                        return Observable.just(streamUrl)
                    } else {
                        return Observable.empty()
                    }
                })
    }

}

class StreamCloudResolver(val httpClient: OkHttpClient) : StreamResolver {

    override fun appliesToUrl(url: String): Boolean {
        return url.contains("streamcloud", true)
    }

    override fun resolveStream(url: String): Observable<String> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Response> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val formValues = FormBody.Builder()
                    val regex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
                    for (i in regex.findAll(content)) {
                        formValues.add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                    }

                    val postRequest = Request.Builder().url(response.request().url()).post(formValues.build()).build()
                    return CallObservable(httpClient.newCall(postRequest))
                })
                .flatMap(fun(response): Observable<String> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val regex = Regex("file: \"(.+?)\",")
                    val streamUrl = regex.find(content)?.groupValues?.get(1)

                    if (streamUrl != null) {
                        return Observable.just(streamUrl)
                    } else {
                        return Observable.empty()
                    }
                })
    }
}