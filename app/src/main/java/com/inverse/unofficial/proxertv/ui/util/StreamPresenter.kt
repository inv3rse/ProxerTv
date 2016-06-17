package com.inverse.unofficial.proxertv.ui.util

import android.graphics.Color
import android.support.v17.leanback.widget.Presenter
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.inverse.unofficial.proxertv.model.Stream

/**
 * Presents a selectable stream url
 */
class StreamPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val textView = TextView(parent.context)
        textView.layoutParams = ViewGroup.LayoutParams(WIDTH, HEIGHT);
        textView.isFocusable = true
        textView.isFocusableInTouchMode = true
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.GRAY)
        textView.gravity = Gravity.CENTER

        return ViewHolder(textView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        (viewHolder.view as TextView).text = (item as Stream).providerName
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    companion object {
        private const val WIDTH = 200
        private const val HEIGHT = 200
    }
}