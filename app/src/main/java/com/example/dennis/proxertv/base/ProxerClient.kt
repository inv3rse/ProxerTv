package com.example.dennis.proxertv.base

import com.example.dennis.proxertv.model.ApiEpisodesInfo
import com.example.dennis.proxertv.model.Series
import com.example.dennis.proxertv.model.SeriesCover
import com.example.dennis.proxertv.model.ServerConfig
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import rx.Observable
import java.util.*

class ProxerClient(
        val httpClient: OkHttpClient,
        val gson: Gson,
        val streamResolvers: List<StreamResolver>,
        val serverConfig: ServerConfig) {

    fun loadTopAccessSeries(forceDownload: Boolean = false): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topAccessListUrl, forceDownload)
    }

    fun loadTopRatingSeries(forceDownload: Boolean = false): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topRatingListUrl, forceDownload)
    }

    fun loadAiringSeries(forceDownload: Boolean = false): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.airingListUrl, forceDownload)
    }

    fun loadSeries(id: Int): Observable<Series?> {
        val request = Request.Builder().get().url(serverConfig.detailUrl(id)).build()

        return CallObservable(httpClient.newCall(request))
                .map(fun(response): Series? {
                    val body = response.body()
                    val soup = Jsoup.parse(body.byteStream(), "UTF-8", serverConfig.baseUrl)
                    body.close()

                    val tableElement = soup.select(".details>tbody")?.first()?.children()
                    if (tableElement != null && tableElement.size >= 3) {
                        var title: String? = null
                        var engTitle: String? = null
                        var description: String? = null

                        tableElement.forEach {
                            if (it.children().size >= 2) {
                                when (it.child(0).text()) {
                                    "Original Titel" -> title = it.child(1).text()
                                    "Eng. Titel" -> engTitle = it.child(1).text()
                                }
                            } else if (it.children().size == 1) {
                                description = it.child(0).text()
                            }
                        }

                        if (title != null && description != null) {
                            if (engTitle == null) {
                                engTitle = title
                            }

                            return Series(id, title!!, engTitle!!, description!!, serverConfig.coverUrl(id))
                        }
                    }

                    return null
                })
                .flatMap({ loadNumEpisodes(it) })
    }

    fun loadEpisodeStreams(seriesId: Int, episode: Int, subType: String): Observable<String> {
        val request = Request.Builder().get().url(serverConfig.episodeStreamsUrl(seriesId, episode, subType)).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<String> {
                    val body = response.body()
                    val content = body.string()
                    body.close()

                    val embedUrls = arrayListOf<String>()
                    val regex = Regex("<script type=\"text/javascript\">\n\n.*var streams = (\\[.*\\])")
                    val findResult = regex.find(content)
                    if (findResult != null) {
                        val json = findResult.groups[1]?.value
                        if (json != null) {
                            val mapped = gson.fromJson<List<Map<String, String>>>(json)
                            mapped.forEach {
                                val url = it["replace"]
                                val code = it["code"]
                                if (url != null && code != null) {
                                    embedUrls.add(if (url.isEmpty()) code else url.replace("#", code))
                                }
                            }
                        }
                    }

                    return Observable.from(embedUrls)
                })
                // resolve the link to the stream provider to the actual video stream
                .flatMap(fun(providerUrl): Observable<String> {

                    val resolveObservables = streamResolvers
                            .filter { it.appliesToUrl(providerUrl) }
                            .map { it.resolveStream(providerUrl) }

                    return Observable.mergeDelayError(resolveObservables)
                })
    }

    private fun loadSeriesList(url: String, forceDownload: Boolean): Observable<List<SeriesCover>> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .map(fun(response): List<SeriesCover> {
                    val body = response.body()
                    val soup = Jsoup.parse(body.byteStream(), "UTF-8", serverConfig.baseUrl)
                    body.close()

                    val elements = soup.select("#box-table-a>tbody>tr>td>a")

                    val seriesCovers = arrayListOf<SeriesCover>()
                    elements.forEach {
                        try {
                            val id = it.attr("href").substringAfter("/info/").substringBefore('#').toInt()
                            val title = it.text()
                            seriesCovers.add(SeriesCover(id, title, serverConfig.coverUrl(id)))
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }

                    return seriesCovers;
                })
    }

    private fun loadNumEpisodes(series: Series?): Observable<Series?> {
        if (series == null) {
            return Observable.just(null)
        }

        val request = Request.Builder().get().url(serverConfig.episodesListJsonUrl(series.id)).build()

        return CallObservable(httpClient.newCall(request))
                .map(fun(response): Series {
                    val body = response.body()
                    val info: ApiEpisodesInfo? = gson.fromJson(body.string(), ApiEpisodesInfo::class.java)
                    body.close()

                    val episodeMap = hashMapOf<String, ArrayList<Int>>()
                    if (info != null) {
                        for (episode in info.data) {
                            episodeMap.getOrPut(episode.typ, { arrayListOf<Int>() }).add(episode.no)
                        }
                    }

                    return series.copy(availAbleEpisodes = episodeMap)
                })
    }
}