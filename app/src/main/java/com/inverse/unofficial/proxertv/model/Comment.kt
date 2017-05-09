package com.inverse.unofficial.proxertv.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the comment ratings as delivered via the list api
 */
data class CommentRatings(
        @SerializedName("genre")
        val ratingGenre: Int?,
        @SerializedName("story")
        val ratingStory: Int?,
        @SerializedName("animation")
        val ratingAnimation: Int?,
        @SerializedName("characters")
        val ratingCharacters: Int?,
        @SerializedName("music")
        val ratingMusic: Int?
)