package com.inverse.unofficial.proxertv.ui.util

import androidx.leanback.widget.ObjectAdapter
import com.inverse.unofficial.proxertv.model.Stream

/**
 * Custom Adapter that handles Streams and their selection/failed state
 */
class StreamAdapter : ObjectAdapter(StreamPresenter()) {
    private val streams = mutableListOf<Stream>()
    private val failedStreams = mutableListOf<Stream>()
    private var currentStream: Stream? = null

    override fun get(position: Int): StreamHolder? {
        val stream = streams[position]
        return StreamHolder(stream, failedStreams.contains(stream), currentStream == stream)
    }

    override fun size(): Int {
        return streams.size
    }

    fun setStreams(streams: List<Stream>) {
        this.streams.clear()
        this.streams.addAll(streams)
        notifyChanged()
    }

    fun addFailed(stream: Stream) {
        ifExistent(stream) { index ->
            failedStreams.add(stream)
            notifyItemRangeChanged(index, 1)
        }
    }

    fun removeFailed(stream: Stream) {
        ifExistent(stream) { index ->
            if (failedStreams.remove(stream)) {
                if (currentStream == stream) {
                    currentStream = null
                }
                notifyItemRangeChanged(index, 1)
            }
        }
    }

    fun setCurrentStream(stream: Stream) {
        ifExistent(stream) { newIndex ->
            val oldIndex = if (currentStream != null) streams.indexOf(currentStream!!) else null
            currentStream = stream

            if (oldIndex != null) {
                notifyItemRangeChanged(oldIndex, 1)
            }
            notifyItemRangeChanged(newIndex, 1)
        }
    }

    fun getCurrentStream(): Stream? {
        return currentStream
    }

    fun clear() {
        streams.clear()
        failedStreams.clear()
        currentStream = null
        notifyChanged()
    }

    private fun ifExistent(stream: Stream, withIndex: (index: Int) -> Unit) {
        val index = streams.indexOf(stream)
        if (index != -1) {
            withIndex(index)
        }
    }

    /**
     * Data class that holds the information for presenting a [Stream]
     */
    data class StreamHolder(
            val stream: Stream,
            val failed: Boolean,
            val current: Boolean)
}