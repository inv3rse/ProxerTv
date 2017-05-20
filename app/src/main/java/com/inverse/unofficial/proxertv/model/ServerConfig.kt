package com.inverse.unofficial.proxertv.model

class ServerConfig(
        schema: String = "https",
        val host: String = "proxer.me") {
    val baseUrl = "$schema://$host/"
    val apiBaseUrl = baseUrl + "api/v1/"
    val updatesListUrl = baseUrl + "anime/updates#top"

    fun topAccessListUrl(page: Int = 1) = baseUrl + "anime/animeseries/clicks/all/$page#top"
    fun topRatingListUrl(page: Int = 1) = baseUrl + "anime/animeseries/rating/all/$page#top"
    fun airingListUrl(page: Int = 1) = baseUrl + "anime/airing/rating/all/$page#top"
    fun topRatingMovieListUrl(page: Int = 1) = baseUrl + "anime/movie/rating/all/$page#top"

    fun searchUrl(query: String) = baseUrl + "search?s=search&name=$query&typ=all-anime"
    fun episodeStreamsUrl(seriesId: Int, episodeNum: Int, subType: String) = baseUrl + "watch/$seriesId/$episodeNum/$subType"

    companion object {
        // for ease of use
        fun coverUrl(seriesId: Int) = "https://cdn.proxer.me/cover/$seriesId.jpg"
    }
}
