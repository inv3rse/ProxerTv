package com.inverse.unofficial.proxertv.ui.util

import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.inverse.unofficial.proxertv.R

/**
 * Object that represents a loading cover
 */
object LoadingCover

/**
 * Presents a [LoadingCover]
 */
class LoadingCoverPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_loading_cover, parent, false)
        val card = view.findViewById<ImageCardView>(R.id.loading_cover_image_card)
        card.setMainImageDimensions(BaseCoverPresenter.CARD_WIDTH, BaseCoverPresenter.CARD_HEIGHT)
        return LoadingViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {}

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    private class LoadingViewHolder(view: View) : Presenter.ViewHolder(view)
}