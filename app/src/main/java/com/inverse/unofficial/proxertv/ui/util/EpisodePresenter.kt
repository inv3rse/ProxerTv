package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.R

class EpisodePresenter : BaseCoverPresenter() {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val context = viewHolder.view.context
        val episodeHolder = item as EpisodeAdapter.EpisodeHolder
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = context.getString(R.string.episode, episodeHolder.episode.episodeNum)
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        if (episodeHolder.watched) {
            cardView.badgeImage = context.getDrawable(R.drawable.ic_watched)
        }

        Glide.with(context)
                .load(episodeHolder.episode.coverUrl)
                .centerCrop()
                .into(cardView.mainImageView)
    }
}