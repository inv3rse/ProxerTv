package com.inverse.unofficial.proxertv.model

class ServerConfig(
        schema: String = "https",
        val host: String = "proxer.me") {
    val baseUrl = "$schema://$host/"
    val topAccessListUrl = baseUrl + "anime/animeseries/clicks/all#top"
    val topRatingListUrl = baseUrl + "anime/animeseries/rating/all#top"
    val airingListUrl = baseUrl + "anime/airing#top"
    val topRatingMovieListUrl = baseUrl + "anime/movie/rating/all#top"

    fun searchUrl(query: String) = baseUrl + "search?s=search&name=$query&typ=all-anime"
    fun detailUrl(seriesId: Int) = baseUrl + "info/$seriesId"
    fun episodesListUrl(seriesId: Int) = baseUrl + "info/$seriesId/list"
    fun episodesListJsonUrl(seriesId: Int, page: Int) = baseUrl + "info/$seriesId/list/$page?format=json"
    fun episodeStreamsUrl(seriesId: Int, episodeNum: Int, subType: String) = baseUrl + "watch/$seriesId/$episodeNum/$subType"
    fun coverUrl(seriesId: Int) = "https://cdn.proxer.me/cover/$seriesId.jpg"
}
