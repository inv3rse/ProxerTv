package com.inverse.unofficial.proxertv.model

/**
 * A series with just the essential Information.
 * For a more complex series use [Series]
 */
data class SeriesCover(
        val id: Int,
        val title: String,
        val cid: Long = NO_COMMENT_ID) {

    companion object {
        const val NO_COMMENT_ID = 1L
    }
}