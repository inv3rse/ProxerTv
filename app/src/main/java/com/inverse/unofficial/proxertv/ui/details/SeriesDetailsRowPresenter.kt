package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HorizontalGridView
import android.support.v17.leanback.widget.ItemBridgeAdapter
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
import com.inverse.unofficial.proxertv.ui.details.SeriesDetailsRowPresenter.SeriesDetailsRow

/**
 * Presenter for a [SeriesDetailsRow]. The layout is based on the
 * [android.support.v17.leanback.widget.DetailsOverviewRowPresenter].
 */
class SeriesDetailsRowPresenter : RowPresenter() {

    val pagePresenter = PageSelectionPresenter()

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

        if (item is SeriesDetailsRow) {
            val holder = vh as DetailsViewHolder
            holder.titleTextView.text = item.series.name
            holder.descriptionTextView.text = item.series.description

            if (item.series.pages() > 1) {
                holder.pagesView.visibility = View.VISIBLE

                val pagesAdapter = ArrayObjectAdapter(pagePresenter)
                val pageList = IntProgression.fromClosedRange(1, item.series.pages(), 1).map(::PageSelection)
                pagesAdapter.addAll(0, pageList)

                val bridgeAdapter = ItemBridgeAdapter(pagesAdapter)

                bridgeAdapter.setAdapterListener(object : ItemBridgeAdapter.AdapterListener() {
                    override fun onBind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onBind(viewHolder)
                        viewHolder.presenter.setOnClickListener(
                                viewHolder.viewHolder,
                                { item.pageSelectionListener?.onPageSelected(item, (viewHolder.item as PageSelection)) })
                    }

                    override fun onUnbind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onUnbind(viewHolder)
                        viewHolder.presenter.setOnClickListener(viewHolder.viewHolder, null)
                    }
                })

                holder.pagesGridView.adapter = bridgeAdapter
            } else {
                holder.pagesView.visibility = View.INVISIBLE
            }

            Glide.with(holder.view.context)
                    .load(ServerConfig.coverUrl(item.series.id))
                    .centerCrop()
                    .into(holder.coverImageView)
        }
    }

    override fun onUnbindRowViewHolder(vh: ViewHolder) {
        Glide.clear((vh as DetailsViewHolder).coverImageView)
        super.onUnbindRowViewHolder(vh)
    }

    /**
     * Presentable series details row
     */
    data class SeriesDetailsRow(
            val series: Series,
            var pageSelectionListener: PageSelectedLister? = null)

    /**
     * Interface for page selection click events
     */
    interface PageSelectedLister {
        fun onPageSelected(seriesRow: SeriesDetailsRow, selection: PageSelection)
    }

    /**
     * ViewHolder for the series details row
     */
    private class DetailsViewHolder(view: View) : RowPresenter.ViewHolder(view) {
        val coverImageView = view.findViewById(R.id.series_details_cover) as ImageView
        val titleTextView = view.findViewById(R.id.series_detail_title) as TextView
        val descriptionTextView = view.findViewById(R.id.series_detail_description) as TextView
        val pagesView: View = view.findViewById(R.id.series_detail_pages_view)
        val pagesGridView = view.findViewById(R.id.series_detail_pages) as HorizontalGridView
    }
}