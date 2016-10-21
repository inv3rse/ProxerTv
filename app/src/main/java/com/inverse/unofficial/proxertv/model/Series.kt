package com.inverse.unofficial.proxertv.model

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

/**
 * Represents a series
 */
@PaperParcel
data class Series(
        val id: Int,
        val name: String,
        val description: String = "",
        val count: Int,
        val state: Int,
        val cid: Long = SeriesCover.NO_COMMENT_ID) : PaperParcelable {

    fun pages(): Int {
        return Math.ceil(count.toDouble() / ProxerClient.EPISODES_PER_PAGE).toInt()
    }

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Series::class.java)

        const val STATE_USER_FINISHED = 0
        const val STATE_USER_WATCHING = 1
        const val STATE_USER_BOOKMARKED = 2
        const val STATE_USER_ABORTED = 3
    }
}