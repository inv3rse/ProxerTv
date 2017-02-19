package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.RowPresenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.ServerConfig

/**
 * Presenter for a series details row. The layout is based on the
 * [android.support.v17.leanback.widget.DetailsOverviewRowPresenter].
 */
class SeriesDetailsRowPresenter : RowPresenter() {

    init {
        headerPresenter = null
        selectEffectEnabled = false
    }

    override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_series_details, parent, false)
        return DetailsViewHolder(view)
    }

    override fun onBindRowViewHolder(vh: ViewHolder, item: Any?) {
        super.onBindRowViewHolder(vh, item)

        if (item is Series) {
            val holder = vh as DetailsViewHolder
            holder.titleTextView.text = item.name
            holder.descriptionTextView.text = item.description

            Glide.with(holder.view.context)
                    .load(ServerConfig.coverUrl(item.id))
                    .centerCrop()
                    .into(holder.coverImageView)
        }
    }

    override fun onUnbindRowViewHolder(vh: ViewHolder) {
        Glide.clear((vh as DetailsViewHolder).coverImageView)
        super.onUnbindRowViewHolder(vh)
    }

    private class DetailsViewHolder(view: View) : RowPresenter.ViewHolder(view) {
        val coverImageView = view.findViewById(R.id.series_details_cover) as ImageView
        val titleTextView = view.findViewById(R.id.series_detail_title) as TextView
        val descriptionTextView = view.findViewById(R.id.series_detail_description) as TextView
    }
}