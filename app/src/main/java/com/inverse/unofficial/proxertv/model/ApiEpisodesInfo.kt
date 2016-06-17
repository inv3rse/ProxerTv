package com.inverse.unofficial.proxertv.model

data class ApiEpisodesInfo(
        val start: String = "",
        val end: String = "",
        val kat: String = "",
        val lang: List<String> = emptyList(),
        val data: List<ApiEpisodeEntry> = emptyList()) {
}

data class ApiEpisodeEntry(
        val no: Int = -1,
        val typ: String = "") {
}