package com.inverse.unofficial.proxertv.model

import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class Episode(
        val seriesId: Int,
        val episodeNum: Int,
        val languageType: String,
        val coverUrl: String) : PaperParcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Episode::class.java)
    }
}