package com.inverse.unofficial.proxertv.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Episode(
        @SerializedName("no")
        val episodeNum: Int,
        @SerializedName("typ")
        val languageType: String
) : Parcelable
