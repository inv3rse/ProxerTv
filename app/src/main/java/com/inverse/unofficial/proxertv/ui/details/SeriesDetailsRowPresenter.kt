package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HorizontalGridView
import android.support.v17.leanback.widget.ItemBridgeAdapter
import android.support.v17.leanback.widget.RowPresenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesList
import com.inverse.unofficial.proxertv.model.ServerConfig
import com.inverse.unofficial.proxertv.ui.details.SeriesDetailsRowPresenter.SeriesDetailsRow
import com.inverse.unofficial.proxertv.ui.util.getStringRes
import kotlin.properties.Delegates

/**
 * Presenter for a [SeriesDetailsRow]. The layout is based on the
 * [android.support.v17.leanback.widget.DetailsOverviewRowPresenter].
 */
class SeriesDetailsRowPresenter(var selectSeriesDetailsRowListener: SeriesDetailsRowListener?) : RowPresenter() {

    val pagePresenter = PageSelectionPresenter()
    var seriesList: SeriesList = SeriesList.NONE

    init {
        headerPresenter = null
        selectEffectEnabled = false
    }

    override fun dispatchItemSelectedListener(vh: ViewHolder?, selected: Boolean) {
        super.dispatchItemSelectedListener(vh, selected)
    }

    override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_series_details, parent, false)
        return DetailsViewHolder(view)
    }

    override fun onBindRowViewHolder(vh: ViewHolder, item: Any?) {
        super.onBindRowViewHolder(vh, item)

        if (item is SeriesDetailsRow) {
            vh as DetailsViewHolder
            // store the item to unbind the page change listener later
            vh.item = item
            vh.titleTextView.text = item.series.name
            vh.descriptionTextView.text = item.series.description
            vh.selectListButton.setText(seriesList.getStringRes())
            vh.selectListButton.setOnClickListener { selectSeriesDetailsRowListener?.onSelectListClicked(item) }

            if (item.series.pages() > 1) {
                vh.pagesView.visibility = View.VISIBLE

                val pagesAdapter = ArrayObjectAdapter(pagePresenter)
                pagesAdapter.addAll(0, createPageSelectionList(item.series.pages(), item.currentPageNumber))

                val bridgeAdapter = ItemBridgeAdapter(pagesAdapter)

                bridgeAdapter.setAdapterListener(object : ItemBridgeAdapter.AdapterListener() {
                    override fun onBind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onBind(viewHolder)
                        viewHolder.presenter.setOnClickListener(
                                viewHolder.viewHolder,
                                { selectSeriesDetailsRowListener?.onPageSelected(item, (viewHolder.item as PageSelection)) })
                    }

                    override fun onUnbind(viewHolder: ItemBridgeAdapter.ViewHolder) {
                        super.onUnbind(viewHolder)
                        viewHolder.presenter.setOnClickListener(viewHolder.viewHolder, null)
                    }
                })

                vh.pagesGridView.adapter = bridgeAdapter

                // react to item page selection change
                vh.pageSelectionChangeListener = item.addPageSelectionChangeListener {
                    vh.pagesGridView.itemAnimator = null // disable the change animation
                    pagesAdapter.clear()
                    pagesAdapter.addAll(0, createPageSelectionList(item.series.pages(), it))
                }
            } else {
                vh.pagesView.visibility = View.INVISIBLE
            }

            Glide.with(vh.view.context)
                    .load(ServerConfig.coverUrl(item.series.id))
                    .centerCrop()
                    .into(vh.coverImageView)
        }
    }

    override fun onUnbindRowViewHolder(vh: ViewHolder) {
        vh as DetailsViewHolder
        Glide.clear(vh.coverImageView)
        // remove the page selection change listener if it exists
        vh.pageSelectionChangeListener?.let { vh.item?.removePageSelectionChangeListener(it) }
        vh.pageSelectionChangeListener = null
        vh.item = null
        super.onUnbindRowViewHolder(vh)
    }

    private fun createPageSelectionList(numPages: Int, currentPage: Int): List<PageSelection> {
        return IntProgression
                .fromClosedRange(1, numPages, 1)
                .map { PageSelection(it, it == currentPage) }
    }

    /**
     * Presentable series details row
     */
    class SeriesDetailsRow(
            val series: Series,
            selectedPageNumber: Int = 1) {

        private val listeners = mutableSetOf<(Int) -> Unit>()

        var currentPageNumber: Int
                by Delegates.observable(selectedPageNumber, { _, _, new -> listeners.forEach { it(new) } })

        fun addPageSelectionChangeListener(listener: (Int) -> Unit): (Int) -> Unit {
            listeners.add(listener)
            return listener
        }

        fun removePageSelectionChangeListener(listener: (Int) -> Unit) {
            listeners.remove(listener)
        }
    }

    /**
     * Interface for page selection click events
     */
    interface SeriesDetailsRowListener {
        fun onSelectListClicked(seriesRow: SeriesDetailsRow)
        fun onPageSelected(seriesRow: SeriesDetailsRow, selection: PageSelection)
    }

    /**
     * ViewHolder for the series details row
     */
    private class DetailsViewHolder(view: View) : RowPresenter.ViewHolder(view) {
        val coverImageView = view.findViewById(R.id.series_details_cover) as ImageView
        val titleTextView = view.findViewById(R.id.series_detail_title) as TextView
        val descriptionTextView = view.findViewById(R.id.series_detail_description) as TextView
        val genresTextView = view.findViewById(R.id.series_details_genres) as TextView
        val pagesView: View = view.findViewById(R.id.series_detail_pages_view)
        val pagesGridView = view.findViewById(R.id.series_detail_pages) as HorizontalGridView
        val selectListButton = view.findViewById(R.id.series_details_select_list_button) as Button

        var item: SeriesDetailsRow? = null
        var pageSelectionChangeListener: ((Int) -> Unit)? = null
    }
}