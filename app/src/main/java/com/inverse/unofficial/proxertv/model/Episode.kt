package com.inverse.unofficial.proxertv.model

import com.google.gson.annotations.SerializedName
import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class Episode(
        @SerializedName("no")
        val episodeNum: Int,
        @SerializedName("typ")
        val languageType: String) : PaperParcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Episode::class.java)
    }
}