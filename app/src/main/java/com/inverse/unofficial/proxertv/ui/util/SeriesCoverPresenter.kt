package com.inverse.unofficial.proxertv.ui.util

import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.ServerConfig

/**
 * Presents an [ISeriesCover] item
 */
class SeriesCoverPresenter : BaseCoverPresenter() {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        viewHolder as CoverViewHolder
        val context = viewHolder.view.context
        val series = item as ISeriesCover

        viewHolder.cardView.titleText = series.name
        viewHolder.cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        Glide.with(context)
                .load(ServerConfig.coverUrl(series.id))
                .centerCrop()
                .into(viewHolder.cardView.mainImageView)
    }
}
