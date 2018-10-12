package com.inverse.unofficial.proxertv.ui.util

import android.graphics.Color
import androidx.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.inverse.unofficial.proxertv.R

/**
 * Presents a StreamHolder, which holds a stream with extra information.
 */
class StreamPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return StreamViewHolder(inflater.inflate(R.layout.view_stream, parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val streamHolder = item as StreamAdapter.StreamHolder
        val streamView = viewHolder as StreamViewHolder

        val color = if (streamHolder.failed) Color.RED else Color.GRAY
        streamView.textView.text = streamHolder.stream.providerName
        streamView.view.setBackgroundColor(color)
        streamView.activeView.visibility = if (streamHolder.current) View.VISIBLE else View.INVISIBLE
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    /**
     * ViewHolder for a [StreamAdapter.StreamHolder]
     */
    internal class StreamViewHolder(view: View) : Presenter.ViewHolder(view) {
        val textView = view.findViewById(R.id.stream_label) as TextView
        val activeView: View = view.findViewById(R.id.stream_active_indicator)
    }
}