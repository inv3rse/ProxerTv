package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ObjectAdapter
import com.inverse.unofficial.proxertv.model.Episode

class EpisodeAdapter(progress: Int, presenter: EpisodePresenter) : ObjectAdapter(presenter) {
    private val episodes = arrayListOf<Episode>()
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

    fun add(episode: Episode) {
        episodes.add(episode)
        notifyItemRangeInserted(episodes.size - 1, 1)
    }

    data class EpisodeHolder(
            val episode: Episode,
            val watched: Boolean
    )
}