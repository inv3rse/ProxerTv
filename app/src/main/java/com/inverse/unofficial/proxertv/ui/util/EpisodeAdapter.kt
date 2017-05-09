package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ObjectAdapter
import com.inverse.unofficial.proxertv.model.Episode

class EpisodeAdapter(
        private val episodes: List<Episode>,
        progress: Int,
        presenter: EpisodePresenter) : ObjectAdapter(presenter) {

    var progress = progress
        set(value) {
            field = value
            notifyItemRangeChanged(0, episodes.size)
        }

    override fun size(): Int {
        return episodes.size
    }

    override fun get(position: Int): Any {
        val episode = episodes[position]
        return EpisodeHolder(episode, episode.episodeNum <= progress)
    }

    data class EpisodeHolder(
            val episode: Episode,
            val watched: Boolean
    )
}