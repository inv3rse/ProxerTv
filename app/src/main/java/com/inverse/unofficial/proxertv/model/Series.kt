package com.inverse.unofficial.proxertv.model

data class Series(
        val id: Int,
        val originalTitle: String,
        val englishTitle: String = originalTitle,
        val description: String = "",
        val imageUrl: String = "",
        val pages: Int = 1) {
}