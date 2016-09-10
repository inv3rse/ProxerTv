package com.inverse.unofficial.proxertv.base.client

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.base.client.util.CallObservable
import com.inverse.unofficial.proxertv.base.client.util.StreamResolver
import com.inverse.unofficial.proxertv.base.client.util.containsCaptcha
import com.inverse.unofficial.proxertv.model.*
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import rx.Observable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ProxerClient(
        val httpClient: OkHttpClient,
        val api: ProxerApi,
        val gson: Gson,
        val streamResolvers: List<StreamResolver>,
        val serverConfig: ServerConfig) {

    init {
        // set adult content cookie
        val url = HttpUrl.parse(serverConfig.baseUrl)
        val adultCookie = Cookie.Builder().hostOnlyDomain(url.host()).path("/").name("adult")
                .value("1").build()
        httpClient.cookieJar().saveFromResponse(url, listOf(adultCookie))
    }

    fun loadTopAccessSeries(): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topAccessListUrl)
    }

    fun loadTopRatingSeries(): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topRatingListUrl)
    }

    fun loadTopRatingMovies(): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topRatingMovieListUrl)
    }

    fun loadAiringSeries(): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.airingListUrl)
    }

    fun loadUpdatesList(): Observable<List<SeriesUpdate>> {
        val request = Request.Builder().get().url(serverConfig.updatesListUrl).build()

        return CallObservable(httpClient.newCall(request))
                .map(fun(response): List<SeriesUpdate> {
                    val body = response.body()
                    val soup = Jsoup.parse(body.byteStream(), "UTF-8", serverConfig.baseUrl)
                    body.close()

                    val idRegex = Regex("/info/(\\d+)(/list)?#top")
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                    val seriesUpdates = arrayListOf<SeriesUpdate>()

                    val rowElements = soup.select("#box-table-a>tbody>tr")

                    rowElements.forEach {
                        val nameLinkElement = it.select("a").first()
                        val dateElement = it.child(5)

                        if (nameLinkElement != null && dateElement != null) {
                            val id = idRegex.find(nameLinkElement.attr("href"))?.groupValues?.get(1)?.toInt()
                            val title = nameLinkElement.text()

                            if (id != null) {
                                try {
                                    val date = dateFormat.parse(dateElement.text())
                                    seriesUpdates.add(SeriesUpdate(SeriesCover(id, title), date))
                                } catch (e: ParseException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    return seriesUpdates
                })
    }

    fun searchSeries(query: String): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.searchUrl(query))
    }

    /**
     * Returns the available episodes by sub/dub type
     */
    fun loadEpisodesPage(seriesId: Int, page: Int): Observable<Map<String, List<Int>>> {
        return api.entryEpisodes(seriesId, page, EPISODES_PER_PAGE)
                .map(fun(episodesData): Map<String, List<Int>> {
                    val episodeMap = hashMapOf<String, ArrayList<Int>>()
                    for ((episodeNum, languageType) in episodesData.episodes) {
                        episodeMap.getOrPut(languageType, { arrayListOf<Int>() }).add(episodeNum)
                    }

                    return episodeMap
                })
    }

    fun loadSeries(id: Int): Observable<Series> {
        return api.entryInfo(id)
    }

    fun loadEpisodeStreams(seriesId: Int, episode: Int, subType: String): Observable<Stream> {
        val request = Request.Builder().get().url(serverConfig.episodeStreamsUrl(seriesId, episode, subType)).build()

        return CallObservable(httpClient.newCall(request))
                .flatMap(fun(response): Observable<Stream> {
                    val content = response.body().string()
                    if (containsCaptcha(content)) {
                        throw SeriesCaptchaException()
                    }

                    val unresolvedStreams = arrayListOf<Stream>()
                    val regex = Regex("<script type=\"text/javascript\">\n\n.*var streams = (\\[.*\\])")
                    val findResult = regex.find(content)
                    if (findResult != null) {
                        val json = findResult.groups[1]?.value
                        if (json != null) {
                            val mapped = gson.fromJson<List<Map<String, String>>>(json)
                            mapped.forEach {
                                val url = it["replace"]
                                val code = it["code"]
                                val providerName = it["name"] ?: it["type"]
                                if (url != null && code != null && providerName != null) {
                                    val unresolvedUrl = if (url.isEmpty()) code else url.replace("#", code)
                                    unresolvedStreams.add(Stream(unresolvedUrl, providerName))
                                }
                            }
                        }
                    }

                    return Observable.from(unresolvedStreams)
                })
                // resolve the link to the stream provider to the actual video stream
                .flatMap(fun(unresolvedStream): Observable<Stream> {

                    val resolveObservables = streamResolvers
                            .filter { it.appliesToUrl(unresolvedStream.streamUrl) }
                            .map { it.resolveStream(unresolvedStream.streamUrl) }

                    return Observable.mergeDelayError(resolveObservables)
                })
    }

    private fun loadSeriesList(url: String): Observable<List<SeriesCover>> {
        val request = Request.Builder().get().url(url).build()

        return CallObservable(httpClient.newCall(request))
                .map(fun(response): List<SeriesCover> {
                    val body = response.body()
                    val soup = Jsoup.parse(body.byteStream(), "UTF-8", serverConfig.baseUrl)
                    body.close()

                    val elements = soup.select("#box-table-a>tbody>tr>td>a")

                    val seriesCovers = arrayListOf<SeriesCover>()
                    val idRegex = Regex("/info/(\\d+)(/list)?#top")
                    elements.forEach {
                        val id = idRegex.find(it.attr("href"))?.groupValues?.get(1)?.toInt()
                        val title = it.text()
                        if (id != null) {
                            seriesCovers.add(SeriesCover(id, title))
                        }
                    }

                    return seriesCovers
                })
    }

    class SeriesCaptchaException : Exception("Content not accessible due to captcha")

    companion object {
        const val EPISODES_PER_PAGE = 50

        fun getTargetPageForEpisode(episodeNum: Int): Int {
            return episodeNum / EPISODES_PER_PAGE
        }
    }
}