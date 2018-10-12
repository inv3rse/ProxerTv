package com.inverse.unofficial.proxertv.ui.details

import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.SeriesList
import com.inverse.unofficial.proxertv.ui.util.getStringRes

data class SeriesListOption(
        val list: SeriesList,
        val selected: Boolean,
        val loading: Boolean
)

interface SeriesListSelectionListener {
    fun listSelected(seriesList: SeriesList)
}

class SeriesListOptionPresenter(private val selectionListener: SeriesListSelectionListener) : Presenter() {

    private val clickListener = View.OnClickListener {
        selectionListener.listSelected(it.tag as SeriesList)
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_side_menu_series_list, parent, false)
        return SeriesListOptionViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        viewHolder as SeriesListOptionViewHolder
        item as SeriesListOption

        viewHolder.view.tag = item.list
        viewHolder.name.setText(item.list.getStringRes())

        viewHolder.checkbox.visibility = if (item.selected && !item.loading) View.VISIBLE else View.INVISIBLE
        viewHolder.progressBar.visibility = if (item.loading) View.VISIBLE else View.INVISIBLE
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    private class SeriesListOptionViewHolder(view: View, clickListener: View.OnClickListener) : Presenter.ViewHolder(view) {
        val checkbox: View = view.findViewById(R.id.series_list_checkbox)
        val progressBar: View = view.findViewById(R.id.series_list_progressbar)
        val name = view.findViewById(R.id.series_list_name) as TextView

        init {
            view.setOnClickListener(clickListener)
        }
    }
}

class SeriesListOptionAdapter(
        selectionListener: SeriesListSelectionListener,
        startState: SeriesList = SeriesList.NONE) : ObjectAdapter(SeriesListOptionPresenter(selectionListener)) {

    var currentState = startState
    var loadingList: SeriesList? = null

    override fun size(): Int {
        return LIST_OPTIONS.size
    }

    override fun get(position: Int): Any {
        val state = LIST_OPTIONS[position]
        return SeriesListOption(state, state == currentState, state == loadingList)
    }

    companion object {
        private val LIST_OPTIONS = listOf(SeriesList.NONE, SeriesList.WATCHLIST, SeriesList.FINISHED, SeriesList.ABORTED)
    }
}