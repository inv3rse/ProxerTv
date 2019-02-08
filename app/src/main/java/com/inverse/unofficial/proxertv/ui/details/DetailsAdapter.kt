package com.inverse.unofficial.proxertv.ui.details

import androidx.leanback.widget.*
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.ui.util.EpisodeAdapter
import com.inverse.unofficial.proxertv.ui.util.EpisodePresenter
import com.inverse.unofficial.proxertv.ui.util.GlideRequests

/**
 * Adapter that shows [seriesDetails] and a list of [episodes]
 */
class DetailsAdapter(
    glide: GlideRequests,
    selectSeriesDetailsRowListener: SeriesDetailsRowPresenter.SeriesDetailsRowListener,
    seriesId: Int,
    coverLoadListener: CoverLoadListener
) : ObjectAdapter() {

    private val episodePresenter = EpisodePresenter(glide, seriesId)
    private val episodeAdapters = mutableMapOf<String, EpisodeAdapter>()

    init {
        val presenterSelector = ClassPresenterSelector()
        presenterSelector.addClassPresenter(
            DetailsData::class.java,
            SeriesDetailsRowPresenter(glide, selectSeriesDetailsRowListener, coverLoadListener)
        )
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        setPresenterSelector(presenterSelector)
    }

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

    var episodes = emptyList<EpisodeCategory>()
        set(value) {
            val oldCount = field.size
            field = value

            var changed = oldCount != value.count()
            value.forEach { category ->
                val adapter = episodeAdapters[category.title]
                val episodes = category.episodes.map { num -> Episode(num, category.title) }

                if (adapter == null) {
                    episodeAdapters[category.title] = EpisodeAdapter(episodes, category.progress, episodePresenter)
                    changed = true
                } else {
                    adapter.episodes = episodes
                    adapter.progress = category.progress
                }
            }

            if (changed) {
                notifyChanged()
            }
        }

    override fun size(): Int {
        return (if (seriesDetails != null) 1 else 0) + episodes.size
    }

    override fun get(position: Int): Any {
        val series = seriesDetails

        return if (series != null) {
            if (position == 0) series else getEpisodesRow(position - 1)
        } else {
            getEpisodesRow(position)
        }
    }

    private fun getEpisodesRow(position: Int): ListRow {
        val category = episodes[position]

        val header = HeaderItem(category.title)
        val adapter = episodeAdapters[category.title]

        return ListRow(position.toLong(), header, adapter)
    }
}