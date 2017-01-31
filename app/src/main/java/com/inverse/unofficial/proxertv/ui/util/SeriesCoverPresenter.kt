package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.ServerConfig

/**
 * Presents an [ISeriesCover] item
 */
class SeriesCoverPresenter : BaseCoverPresenter() {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val context = viewHolder.view.context
        val series = item as ISeriesCover

        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = series.name
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        Glide.with(context)
                .load(ServerConfig.coverUrl(series.id))
                .centerCrop()
                .into(cardView.mainImageView)
    }
}
