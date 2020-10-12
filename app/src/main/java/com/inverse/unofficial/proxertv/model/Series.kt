package com.inverse.unofficial.proxertv.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import kotlinx.android.parcel.Parcelize
import kotlin.math.ceil


/**
 * A [SeriesCover] interface to get around the data class inheritance restriction.
 */
interface ISeriesCover {
    val id: Int
    val name: String
}

/**
 * A series with just the essential Information.
 * For a more complex series use [Series]
 */
data class SeriesCover(
    override val id: Int,
    override val name: String
) : ISeriesCover

/**
 * The possible lists a series can be on. If the list is NONE, then the entry is not in the db.
 */
enum class SeriesList {
    NONE, ABORTED, WATCHLIST, FINISHED;

    companion object {

        /**
         * Returns the [SeriesList] enum by its ordinal value.
         * @param ordinal the ordinal value
         * @return the matching ordinal value or [NONE]
         */
        fun fromOrdinal(ordinal: Int): SeriesList {
            return if (ordinal >= NONE.ordinal && ordinal <= FINISHED.ordinal) {
                values()[ordinal]
            } else {
                NONE
            }
        }

        fun fromApiState(state: Int): SeriesList {
            return when (state) {
                UserListSeriesEntry.STATE_USER_WATCHING, UserListSeriesEntry.STATE_USER_BOOKMARKED -> WATCHLIST
                UserListSeriesEntry.STATE_USER_FINISHED -> FINISHED
                UserListSeriesEntry.STATE_USER_ABORTED -> ABORTED
                else -> NONE
            }
        }

        fun toApiState(list: SeriesList): Int {
            return when (list) {
                WATCHLIST -> UserListSeriesEntry.STATE_USER_BOOKMARKED
                FINISHED -> UserListSeriesEntry.STATE_USER_FINISHED
                ABORTED -> UserListSeriesEntry.STATE_USER_ABORTED
                else -> -1
            }
        }
    }
}

/**
 * A [SeriesDbEntry] interface to get around the data class inheritance restriction.
 */
interface ISeriesDbEntry : ISeriesCover {
    val userList: SeriesList
    val cid: Long
}

/**
 * A [SeriesCover] with a comment id and the list it is stored on.
 */
data class SeriesDbEntry(
    override val id: Int,
    override val name: String,
    override val userList: SeriesList,
    override val cid: Long = NO_COMMENT_ID
) : ISeriesDbEntry {

    companion object {
        const val NO_COMMENT_ID = -1L
    }
}

/**
 * Represents a complete Series
 */
@Parcelize
data class Series(
    @SerializedName("id")
    override val id: Int,
    @SerializedName("name")
    override val name: String,
    @SerializedName("genre")
    val genres: String,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("count")
    val count: Int,
    @SerializedName("state")
    val state: Int
) : Parcelable, ISeriesCover {

    fun pages(): Int {
        return ceil(count.toDouble() / ProxerClient.EPISODES_PER_PAGE).toInt()
    }
}

/**
 * The model returned by the user list api
 */
data class UserListSeriesEntry(
    @SerializedName("medium")
    val medium: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("count")
    val count: Int,
    @SerializedName("cid")
    val cid: Long,
    @SerializedName("state")
    val commentState: Int,
    @SerializedName("episode")
    val episode: Int,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("comment")
    val comment: String,
    @SerializedName("data")
    val commentRating: CommentRatings?
) {

    companion object {
        // ------------ User list state ------------
        const val STATE_USER_FINISHED = 0
        const val STATE_USER_WATCHING = 1
        const val STATE_USER_BOOKMARKED = 2
        const val STATE_USER_ABORTED = 3
    }
}
