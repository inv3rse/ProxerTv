package com.inverse.unofficial.proxertv.ui.util

import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import android.view.ViewGroup
import com.bumptech.glide.Glide

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
abstract class BaseCoverPresenter(protected val glide: GlideRequests) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return CoverViewHolder(cardView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        // Remove references to images so that the garbage collector can free up memory
        glide.clear(cardView.mainImageView)
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    class CoverViewHolder(val cardView: ImageCardView) : Presenter.ViewHolder(cardView)

    companion object {
        const val CARD_WIDTH = 280
        const val CARD_HEIGHT = 392
    }
}