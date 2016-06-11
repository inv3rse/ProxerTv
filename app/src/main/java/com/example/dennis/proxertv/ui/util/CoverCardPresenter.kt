package com.example.dennis.proxertv.ui.util

import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.Presenter
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.dennis.proxertv.model.Episode
import com.example.dennis.proxertv.model.SeriesCover

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
class CoverCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val series = if (item is Episode)
            SeriesCover(item.seriesId, "Episode ${item.episodeNum}", item.coverUrl) else
            item as SeriesCover

        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = series.title
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        Glide.with(viewHolder.view.context)
                .load(series.coverImage)
                .centerCrop()
                .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        Glide.clear(cardView.mainImageView)
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    companion object {
        private val CARD_WIDTH = 280
        private val CARD_HEIGHT = 392
    }
}
