package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.inverse.unofficial.proxertv.R

/**
 * Data class that holds the selectable page number.
 */
data class PageSelection(
        val pageNumber: Int)

/**
 * Presenter for a [PageSelection].
 */
class PageSelectionPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_series_page_selection, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val holder = viewHolder as ActionViewHolder
        val page = item as PageSelection

        holder.button.text = page.pageNumber.toString()
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    private class ActionViewHolder(view: View) : ViewHolder(view) {
        val button = view.findViewById(R.id.action_button) as Button
    }
}