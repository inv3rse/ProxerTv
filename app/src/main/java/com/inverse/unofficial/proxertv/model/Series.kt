package com.inverse.unofficial.proxertv.model

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class Series(
        val id: Int,
        val name: String,
        val description: String = "",
        val count: Int) : PaperParcelable {

    fun pages(): Int {
        return Math.ceil(count.toDouble() / ProxerClient.EPISODES_PER_PAGE).toInt()
    }

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Series::class.java)
    }
}