package com.example.dennis.proxertv.model

class ServerConfig(
        val baseUrl: String = "https://proxer.me/",
        val topAccessListUrl: String = baseUrl + "anime/animeseries/clicks/all#top",
        val topRatingListUrl: String = baseUrl + "anime/animeseries/rating/all#top",
        val airingListUrl: String = baseUrl + "anime/airing#top",
        val searchUrl: (query: String) -> String = { query -> baseUrl + "search?s=search&name=$query&typ=all-anime" },
        val detailUrl: (seriesId: Int) -> String = { id -> baseUrl + "info/$id" },
        val episodesListUrl: (seriesId: Int) -> String = { id -> baseUrl + "info/$id/list" },
        val episodesListJsonUrl: (seriesId: Int, page: Int) -> String = { id, page -> baseUrl + "info/$id/list/$page?format=json" },
        val episodeStreamsUrl: (seriesId: Int, episodeNum: Int, subType: String) -> String = {
            seriesId, episodeNum, subType ->
            baseUrl + "watch/$seriesId/$episodeNum/$subType"
        },
        val coverUrl: (seriesId: Int) -> String = { id -> "https://cdn.proxer.me/cover/$id.jpg" }) {
}
