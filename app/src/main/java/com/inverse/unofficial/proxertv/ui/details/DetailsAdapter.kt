package com.inverse.unofficial.proxertv.ui.details

import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import com.inverse.unofficial.proxertv.ui.util.GlideRequests

/**
 * Adapter that shows [seriesDetails] and a list of [episodes]
 */
class DetailsAdapter(
    glide: GlideRequests,
    selectSeriesDetailsRowListener: SeriesDetailsRowPresenter.SeriesDetailsRowListener
) : ObjectAdapter() {

    var seriesDetails: DetailsData? = null
        set(value) {
            val old = field
            field = value

            if (old == null) {
                if (value != null) {
                    notifyItemRangeInserted(0, 1)
                }
            } else {
                if (value == null) {
                    notifyItemRangeRemoved(0, 1)
                } else {
                    notifyItemRangeChanged(0, 1)
                }
            }
        }

    var episodes = emptyList<ListRow>()
        set(value) {
            val removeCount = field.size
            field = value

            val start = if (seriesDetails == null) 0 else 1

            if (removeCount > 0) {
                notifyItemRangeRemoved(start, removeCount)
            }

            if (value.isNotEmpty()) {
                notifyItemRangeInserted(start, value.size)
            }
        }

    private val detailsOverviewPresenter = SeriesDetailsRowPresenter(glide, selectSeriesDetailsRowListener)

    init {
        val presenterSelector = ClassPresenterSelector()
        presenterSelector.addClassPresenter(
            DetailsData::class.java,
            detailsOverviewPresenter
        )
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        setPresenterSelector(presenterSelector)
    }

    override fun size(): Int {
        return (if (seriesDetails != null) 1 else 0) + episodes.size
    }

    override fun get(position: Int): Any {
        val series = seriesDetails

        return if (series != null) {
            if (position == 0) series else episodes[position - 1]
        } else {
            episodes[position]
        }
    }
}