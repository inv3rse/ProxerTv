package com.inverse.unofficial.proxertv.base.client

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.model.Stream
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
    fun resolveStream(url: String): Observable<Stream>
}

class ProxerStreamResolver(val httpClient: OkHttpClient) : StreamResolver {
    private val name = "Proxer-Stream"
    private val regex = Regex("src=\"(.*\\.mp4)\"")

    override fun appliesToUrl(url: String): Boolean {
        return url.contains("stream.proxer.me", true)
    }

    override fun resolveStream(url: String): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Stream> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val streamUrl = regex.find(content)?.groupValues?.get(1)

                    if (streamUrl != null) {
                        return Observable.just(Stream(streamUrl, name))
                    } else {
                        return Observable.empty()
                    }
                })
    }

}

class StreamCloudResolver(val httpClient: OkHttpClient) : StreamResolver {
    private val name = "StreamCloud"
    private val regex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")

    override fun appliesToUrl(url: String): Boolean {
        return url.contains("streamcloud", true)
    }

    override fun resolveStream(url: String): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Response> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val formValues = FormBody.Builder()
                    for (i in regex.findAll(content)) {
                        formValues.add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                    }

                    val postRequest = Request.Builder().url(response.request().url()).post(formValues.build()).build()
                    return CallObservable(httpClient.newCall(postRequest))
                })
                .flatMap(fun(response): Observable<Stream> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val regex = Regex("file: \"(.+?)\",")
                    val streamUrl = regex.find(content)?.groupValues?.get(1)

                    if (streamUrl != null) {
                        return Observable.just(Stream(streamUrl, name))
                    } else {
                        return Observable.empty()
                    }
                })
    }
}

class Mp4UploadStreamResolver(val httpClient: OkHttpClient) : StreamResolver {
    private val name = "Mp4Upload"
    private val regex = Regex("\"file\": \"(.+)\"")

    override fun appliesToUrl(url: String): Boolean {
        return url.contains("mp4upload.com")
    }

    override fun resolveStream(url: String): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Stream> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val streamUrl = regex.find(content)?.groupValues?.get(1)
                    if (streamUrl != null) {
                        return Observable.just(Stream(streamUrl, name))
                    } else {
                        return Observable.empty()
                    }
                })
    }

}

class DailyMotionStreamResolver(val httpClient: OkHttpClient, val gson: Gson) : StreamResolver {
    private val regex = Regex("\"qualities\":(\\{.+\\}\\]\\}),")

    override fun appliesToUrl(url: String): Boolean {
        return url.contains("dailymotion.com")
    }

    override fun resolveStream(url: String): Observable<Stream> {
        // fix missing http  (//www.dailymotion.com/embed/video/someId)
        val fixedUrl = if (url.startsWith("//")) "http:" + url else url
        val request = Request.Builder().get().url(fixedUrl).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Stream> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val streamList = arrayListOf<Stream>()
                    val qualitiesJson = regex.find(content)?.groupValues?.get(1)
                    if (qualitiesJson != null) {
                        val qualityMap = gson.fromJson<Map<String, List<Map<String, String>>>>(qualitiesJson)
                        val sortedQualities = qualityMap.keys.mapNotNull {
                            try {
                                it.toInt()
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }.sortedDescending()

                        for (quality in sortedQualities.take(2)) {
                            val streamUrl = qualityMap[quality.toString()]?.get(0)?.get("url")
                            if (streamUrl != null) {
                                streamList.add(Stream(streamUrl, "DailyMotion\n${quality}p"))
                            }
                        }
                    }

                    return Observable.from(streamList)
                })
    }
}