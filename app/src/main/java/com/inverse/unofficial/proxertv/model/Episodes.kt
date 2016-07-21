package com.inverse.unofficial.proxertv.model

data class Episodes(
        val start: Int,
        val end: Int,
        val lang: List<String>,
        val state: Int,
        val episodes: List<Episode>
)