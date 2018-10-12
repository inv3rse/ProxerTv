package com.inverse.unofficial.proxertv.base.client.util

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.model.Stream
import okhttp3.*
import rx.Observable

interface StreamResolver {
    /**
     * return true if the url might be resolvable
     */
    fun appliesToUrl(url: HttpUrl): Boolean

    /**
     * try to find the stream url
     */
    fun resolveStream(url: HttpUrl): Observable<Stream>
}

class ProxerStreamResolver(private val httpClient: OkHttpClient) : StreamResolver {
    private val name = "Proxer-Stream"
    private val regex = Regex("src=\"(.*\\.mp4)\"")

    override fun appliesToUrl(url: HttpUrl): Boolean {
        return url.host().contains("stream.proxer.me", true)
    }

    override fun resolveStream(url: HttpUrl): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return BodyCallObservable(httpClient.newCall(request))
            .flatMap(fun(body): Observable<Stream> {
                val content = body.string()
                body.close()

                val streamUrl = regex.find(content)?.groupValues?.get(1)

                return if (streamUrl != null) {
                    Observable.just(Stream(streamUrl, name))
                } else {
                    Observable.empty()
                }
            })
    }

}

class StreamCloudResolver(private val httpClient: OkHttpClient) : StreamResolver {
    private val name = "StreamCloud"
    private val regex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")

    override fun appliesToUrl(url: HttpUrl): Boolean {
        return url.host().contains("streamcloud", true)
    }

    override fun resolveStream(url: HttpUrl): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
            .flatMap(fun(response): Observable<ResponseBody> {
                if (!response.isSuccessful) {
                    throw OkHttpException(response)
                }

                val body = response.body() ?: throw EmptyBodyException()
                val content = body.string()
                body.close()

                val formValues = FormBody.Builder()
                for (i in regex.findAll(content)) {
                    formValues.add(
                        i.groupValues[1],
                        i.groupValues[2].replace("download1", "download2")
                    )
                }

                val postRequest =
                    Request.Builder().url(response.request().url()).post(formValues.build()).build()
                return BodyCallObservable(httpClient.newCall(postRequest))
            })
            .flatMap(fun(body): Observable<Stream> {
                val content = body.string()
                body.close()

                val regex = Regex("file: \"(.+?)\",")
                val streamUrl = regex.find(content)?.groupValues?.get(1)

                return if (streamUrl != null) {
                    Observable.just(Stream(streamUrl, name))
                } else {
                    Observable.empty()
                }
            })
    }
}

class Mp4UploadStreamResolver(private val httpClient: OkHttpClient) : StreamResolver {
    private val name = "Mp4Upload"
    private val regex =
        Regex("\\|([a-z0-9]+)\\|getDuration\\|.+\\|([a-z0-9]+)\\|([a-z0-9]+)\\|([a-z0-9]+)\\|(\\d+)\\|setup\\|")

    override fun appliesToUrl(url: HttpUrl): Boolean {
        return url.host().contains("mp4upload.com")
    }

    override fun resolveStream(url: HttpUrl): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return BodyCallObservable(httpClient.newCall(request))
            .flatMap(fun(body): Observable<Stream> {
                val content = body.string()
                body.close()

                val streamUrl = regex.find(content)?.groupValues?.let { group ->
                    val sub = group[1]
                    val fileExtension = group[2]
                    val filename = group[3]
                    val id = group[4]
                    val port = group[5]

                    "https://$sub.mp4upload.com:$port/d/$id/$filename.$fileExtension"
                }

                return if (streamUrl != null) {
                    Observable.just(Stream(streamUrl, name))
                } else {
                    Observable.empty()
                }
            })
    }

}

class DailyMotionStreamResolver(private val httpClient: OkHttpClient, val gson: Gson) :
    StreamResolver {
    private val regex = Regex("\"qualities\":(\\{.+\\}\\]\\}),")

    override fun appliesToUrl(url: HttpUrl): Boolean {
        return url.host().contains("dailymotion.com")
    }

    override fun resolveStream(url: HttpUrl): Observable<Stream> {
        val request = Request.Builder().get().url(url).build()

        return BodyCallObservable(httpClient.newCall(request))
            .flatMap(fun(body): Observable<Stream> {
                val content = body.string()
                body.close()

                val streamList = arrayListOf<Stream>()
                val qualitiesJson = regex.find(content)?.groupValues?.get(1)
                if (qualitiesJson != null) {
                    val qualityMap =
                        gson.fromJson<Map<String, List<Map<String, String>>>>(qualitiesJson)
                    val sortedQualities = qualityMap.keys.asSequence().mapNotNull {
                        try {
                            it.toInt()
                        } catch (e: NumberFormatException) {
                            null
                        }
                    }.sortedDescending().toList()

                    for (quality in sortedQualities.take(2)) {
                        val streamOptions = qualityMap[quality.toString()] ?: emptyList()
                        val stream = streamOptions
                            .first { it["type"]?.startsWith("video") ?: false }

                        val streamUrl = stream["url"]
                        if (streamUrl != null) {
                            streamList.add(Stream(streamUrl, "DailyMotion\n${quality}p"))
                        }
                    }
                }

                return Observable.from(streamList)
            })
    }
}