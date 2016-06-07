package com.example.dennis.proxertv.model

data class Series(
        val id: Int,
        val originalTitle: String,
        val englishTitle: String = originalTitle,
        val description: String = "",
        val imageUrl: String = "",
        // available episodes by sub/dub type
        val availAbleEpisodes: Map<String, List<Int>> = emptyMap()
) {
}