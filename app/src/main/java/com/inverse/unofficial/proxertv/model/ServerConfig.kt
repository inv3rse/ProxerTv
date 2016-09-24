package com.inverse.unofficial.proxertv.model

class ServerConfig(
        schema: String = "https",
        val host: String = "proxer.me") {
    val baseUrl = "$schema://$host/"
    val apiBaseUrl = baseUrl + "api/v1/"
    val topAccessListUrl = baseUrl + "anime/animeseries/clicks/all#top"
    val topRatingListUrl = baseUrl + "anime/animeseries/rating/all#top"
    val airingListUrl = baseUrl + "anime/airing/rating/all#top"
    val topRatingMovieListUrl = baseUrl + "anime/movie/rating/all#top"
    val updatesListUrl = baseUrl + "anime/updates#top"

    fun searchUrl(query: String) = baseUrl + "search?s=search&name=$query&typ=all-anime"
    fun episodeStreamsUrl(seriesId: Int, episodeNum: Int, subType: String) = baseUrl + "watch/$seriesId/$episodeNum/$subType"

    companion object {
        // for ease of use
        fun coverUrl(seriesId: Int) = "https://cdn.proxer.me/cover/$seriesId.jpg"
    }
}
