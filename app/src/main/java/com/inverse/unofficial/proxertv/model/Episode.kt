package com.inverse.unofficial.proxertv.model

data class Episode(
        val seriesId: Int,
        val episodeNum: Int,
        val languageType: String,
        val coverUrl: String) {
}