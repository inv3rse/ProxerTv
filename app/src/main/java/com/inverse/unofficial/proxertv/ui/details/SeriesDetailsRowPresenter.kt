package com.inverse.unofficial.proxertv.ui.details

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.RowPresenter
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.SeriesList
import com.inverse.unofficial.proxertv.model.ServerConfig
import com.inverse.unofficial.proxertv.ui.util.GlideRequests
import com.inverse.unofficial.proxertv.ui.util.getStringRes

/**
 * Presenter for a [DetailsData]. The layout is based on the
 * old android.support.v17.leanback.widget.DetailsOverviewRowPresenter
 */
class SeriesDetailsRowPresenter(
    private val glide: GlideRequests,
    private val selectSeriesDetailsRowListener: SeriesDetailsRowListener
) : RowPresenter() {

    private val pagePresenter = PageSelectionPresenter()
    var seriesList: SeriesList = SeriesList.NONE
    var coverReadyListener: (() -> Unit)? = null

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

        item as DetailsData
        vh as DetailsViewHolder

        vh.bind(item)
    }


    private fun createPageSelectionList(numPages: Int, currentPage: Int): List<PageSelection> {
        return IntProgression
            .fromClosedRange(0, numPages, 1)
            .map { PageSelection(it + 1, it == currentPage) }
    }


    /**
     * Interface for page selection click events
     */
    interface SeriesDetailsRowListener {
        fun onSelectListClicked(seriesRow: DetailsData)
        fun onPageSelected(seriesRow: DetailsData, selection: PageSelection)
    }

    /**
     * ViewHolder for the series details row
     */
    private inner class DetailsViewHolder(view: View) : RowPresenter.ViewHolder(view) {
        private val coverImageView = view.findViewById(R.id.series_details_cover) as ImageView
        private val titleTextView = view.findViewById(R.id.series_detail_title) as TextView
        private val descriptionTextView = view.findViewById(R.id.series_detail_description) as TextView
        private val genresTextView = view.findViewById(R.id.series_details_genres) as TextView
        private val pagesView: View = view.findViewById(R.id.series_detail_pages_view)
        private val pagesGridView = view.findViewById(R.id.series_detail_pages) as HorizontalGridView
        private val selectListButton = view.findViewById(R.id.series_details_select_list_button) as Button

        private var item: DetailsData? = null

        /**
         * Bind the given item
         */
        fun bind(item: DetailsData) {
            this.item = item

            titleTextView.text = item.series.name
            descriptionTextView.text = item.series.description
            genresTextView.text = item.series.genres
            selectListButton.setText(seriesList.getStringRes())
            selectListButton.setOnClickListener { selectSeriesDetailsRowListener.onSelectListClicked(item) }

            if (item.series.pages() > 1) {
                pagesView.visibility = View.VISIBLE

                val pagesAdapter = ArrayObjectAdapter(pagePresenter)
                pagesAdapter.addAll(0, createPageSelectionList(item.series.pages(), item.currentPage))

                val bridgeAdapter = ItemBridgeAdapter(pagesAdapter)

                bridgeAdapter.setAdapterListener(object : ItemBridgeAdapter.AdapterListener() {
                    override fun onBind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onBind(viewHolder)
                        viewHolder.presenter.setOnClickListener(viewHolder.viewHolder) {
                            selectSeriesDetailsRowListener.onPageSelected(item, (viewHolder.item as PageSelection))
                        }
                    }

                    override fun onUnbind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onUnbind(viewHolder)
                        viewHolder.presenter.setOnClickListener(viewHolder.viewHolder, null)
                    }
                })

                pagesGridView.adapter = bridgeAdapter
            } else {
                pagesView.visibility = View.INVISIBLE
            }

            glide
                .load(ServerConfig.coverUrl(item.series.id))
                .apply(RequestOptions().centerCrop())
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        view.post { coverReadyListener?.invoke() }
                        return false
                    }
                })
                .into(coverImageView)
        }
    }
}