package com.inverse.unofficial.proxertv.ui.util

import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.ServerConfig

/**
 * Presents an [ISeriesCover] item
 */
class SeriesCoverPresenter(glide: GlideRequests) : BaseCoverPresenter(glide) {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        viewHolder as CoverViewHolder
        val series = item as ISeriesCover

        viewHolder.cardView.titleText = series.name
        viewHolder.cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        TransitionHelper.setCoverTransitionName(viewHolder.cardView.mainImageView, series.id)
        glide.load(ServerConfig.coverUrl(series.id))
            .centerCrop()
            .into(viewHolder.cardView.mainImageView)
    }
}
