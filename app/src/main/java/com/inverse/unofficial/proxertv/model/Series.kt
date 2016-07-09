package com.inverse.unofficial.proxertv.model

import nz.bradcampbell.paperparcel.PaperParcel
import nz.bradcampbell.paperparcel.PaperParcelable

@PaperParcel
data class Series(
        val id: Int,
        val originalTitle: String,
        val englishTitle: String = originalTitle,
        val description: String = "",
        val imageUrl: String = "",
        val pages: Int = 1) : PaperParcelable {

    companion object {
        @JvmField val CREATOR = PaperParcelable.Creator(Series::class.java)
    }
}