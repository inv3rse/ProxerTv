package com.inverse.unofficial.proxertv.ui.util

import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.ServerConfig

class EpisodePresenter(seriesId: Int) : BaseCoverPresenter() {
    private val coverUrl = ServerConfig.coverUrl(seriesId)

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        viewHolder as CoverViewHolder
        val context = viewHolder.view.context
        val episodeHolder = item as EpisodeAdapter.EpisodeHolder

        viewHolder.cardView.titleText = context.getString(R.string.episode, episodeHolder.episode.episodeNum)
        viewHolder.cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        if (episodeHolder.watched) {
            viewHolder.cardView.badgeImage = context.getDrawable(R.drawable.ic_watched)
        }

        Glide.with(context)
                .load(coverUrl)
                .centerCrop()
                .into(viewHolder.cardView.mainImageView)
    }
}