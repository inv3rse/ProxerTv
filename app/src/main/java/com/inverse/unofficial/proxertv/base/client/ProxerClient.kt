package com.inverse.unofficial.proxertv.base.client

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.base.client.interceptors.containsCaptcha
import com.inverse.unofficial.proxertv.base.client.util.BodyCallObservable
import com.inverse.unofficial.proxertv.base.client.util.StreamResolver
import com.inverse.unofficial.proxertv.model.*
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import rx.Observable
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Client for proxer.me that combines the usage of the official api and web parsing.
 */
class ProxerClient(
    private val httpClient: OkHttpClient,
    private val api: ProxerApi,
    private val gson: Gson,
    private val streamResolvers: List<StreamResolver>,
    private val serverConfig: ServerConfig
) : ProxerApi by api {

    init {
        // set adult content cookie
        val url = serverConfig.baseUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("invalid base url")
        val adultCookie = Cookie.Builder().hostOnlyDomain(url.host).path("/").name("adult")
            .value("1").build()
        httpClient.cookieJar.saveFromResponse(url, listOf(adultCookie))
    }

    /**
     * Adds the series to the users watchlist.
     * @param seriesId the id of the series
     * @return an [Observable] emitting true or throwing an error
     */
    fun addSeriesToWatchList(seriesId: Int): Observable<Boolean> {
        return api.addSeriesToList(seriesId, "note")
    }

    /**
     * Load the top access series list.
     * @return an [Observable] emitting the series list
     */
    fun loadTopAccessSeries(page: Int = 1): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topAccessListUrl(page))
    }

    /**
     * Load the top rating series list.
     * @return an [Observable] emitting the series list
     */
    fun loadTopRatingSeries(page: Int = 1): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topRatingListUrl(page))
    }

    /**
     * Load the top rating movies list.
     * @return an [Observable] emitting the movies list
     */
    fun loadTopRatingMovies(page: Int = 1): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.topRatingMovieListUrl(page))
    }

    /**
     * Load the top airing series list.
     * @return an [Observable] emitting the series list
     */
    fun loadAiringSeries(page: Int = 1): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.airingListUrl(page))
    }

    /**
     * Load the list of series updates.
     * @return an [Observable] emitting the updates list
     */
    fun loadUpdatesList(): Observable<List<SeriesUpdate>> {
        val request = Request.Builder().get().url(serverConfig.updatesListUrl).build()

        return BodyCallObservable(httpClient.newCall(request))
            .map(fun(body): List<SeriesUpdate> {
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
                                Timber.e(e)
                            }
                        }
                    }
                }
                return seriesUpdates
            })
    }

    /**
     * Query all series by name
     * @param query the name to search for
     * @return an [Observable] emitting the matching series list
     */
    fun searchSeries(query: String): Observable<List<SeriesCover>> {
        return loadSeriesList(serverConfig.searchUrl(query))
    }

    /**
     * Loads the available episodes map by sub/dub type
     * @param seriesId the id of the series
     * @param page the page to load (first page is 0)
     * @return an [Observable] emitting the available episodes by sub/dub type
     */
    fun loadEpisodesPage(seriesId: Int, page: Int): Observable<Map<String, List<Int>>> {
        return api.entryEpisodes(seriesId, page, EPISODES_PER_PAGE)
            .map { episodesData ->
                val episodeMap = hashMapOf<String, MutableList<Int>>()
                for ((episodeNum, languageType) in episodesData.episodes) {
                    episodeMap.getOrPut(languageType) { mutableListOf() }.add(episodeNum)
                }

                episodeMap
            }
    }

    /**
     * Load the series detail information
     * @param id the series id
     * @return an [Observable] emitting the Series
     */
    fun loadSeries(id: Int): Observable<Series> {
        return api.entryInfo(id)
    }

    /**
     * Load and resolve the possible streams for an episode
     * @param seriesId the id of the series
     * @param episode the episode number
     * @param subType the subtype of the episode
     * @return an [Observable] emitting {@link Stream}s in the order they are resolved to the video file
     */
    fun loadEpisodeStreams(seriesId: Int, episode: Int, subType: String): Observable<Stream> {
        val request = Request.Builder().get().url(serverConfig.episodeStreamsUrl(seriesId, episode, subType)).build()

        return BodyCallObservable(httpClient.newCall(request))
            .flatMap(fun(body): Observable<HttpUrl> {
                val content = body.string()
                if (containsCaptcha(content)) {
                    throw SeriesCaptchaException()
                }

                val unresolvedStreams = arrayListOf<HttpUrl>()
                val regex = Regex("<script type=\"text/javascript\">\n\n.*var streams = (\\[.*])")
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
                                val replacedUrl = if (url.isEmpty()) code else url.replace("#", code)
                                // add missing 'http'
                                val fixedUrl = if (replacedUrl.startsWith("//")) "http:$replacedUrl" else replacedUrl
                                val httpUrl = fixedUrl.toHttpUrlOrNull()
                                if (httpUrl != null) {
                                    unresolvedStreams.add(httpUrl)
                                }
                            }
                        }
                    }
                }

                return Observable.from(unresolvedStreams)
            })
            // resolve the link to the stream provider to the actual video stream
            .flatMap { url ->
                val resolveObservables = streamResolvers
                    .asSequence()
                    .filter { it.appliesToUrl(url) }
                    .map { it.resolveStream(url) }
                    .toList()

                Observable.mergeDelayError(resolveObservables)
            }
    }

    private fun loadSeriesList(url: String): Observable<List<SeriesCover>> {
        val request = Request.Builder().get().url(url).build()

        return BodyCallObservable(httpClient.newCall(request))
            .map(fun(body): List<SeriesCover> {
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

        /**
         * Get the target page for the episode num.
         * @param episodeNum the episode number (starting with 1)
         * @return the target page
         */
        fun getTargetPageForEpisode(episodeNum: Int): Int {
            return Math.max(episodeNum - 1, 0) / EPISODES_PER_PAGE
        }
    }
}
