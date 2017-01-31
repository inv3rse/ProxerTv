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
        val ratingCharacters: Int,
        @SerializedName("music")
        val ratingMusic: Int?
)

/**
 * Represents a comment that holds the progress and rating of a [Series]
 */
data class Comment(
        @SerializedName("state")
        val state: Int,
        @SerializedName("episode")
        val episode: Int,
        @SerializedName("rating")
        val rating: Int,
        @SerializedName("comment")
        val comment: String?,
        @SerializedName("misc[genre]")
        val ratingGenre: Int?,
        @SerializedName("misc[story]")
        val ratingStory: Int?,
        @SerializedName("misc[animation]")
        val ratingAnimation: Int?,
        @SerializedName("misc[characters]")
        val ratingCharacters: Int?,
        @SerializedName("misc[music]")
        val ratingMusic: Int?
)