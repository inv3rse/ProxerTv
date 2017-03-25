package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.inverse.unofficial.proxertv.R

class SeriesListOptionPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_side_menu_series_list, parent, false)
        return SeriesListOptionViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {

    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    private class SeriesListOptionViewHolder(view: View) : Presenter.ViewHolder(view) {
        val checkbox: View = view.findViewById(R.id.series_list_checkbox)
        val name = view.findViewById(R.id.series_list_name) as TextView
    }
}
