package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.model.SeriesCover

class SeriesCoverPresenter : BaseCoverPresenter() {

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val context = viewHolder.view.context
        val series = item as SeriesCover

        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = series.title
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        Glide.with(context)
                .load(series.coverImage)
                .centerCrop()
                .into(cardView.mainImageView)
    }
}