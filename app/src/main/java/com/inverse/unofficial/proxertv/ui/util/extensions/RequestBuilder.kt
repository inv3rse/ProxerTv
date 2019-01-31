package com.inverse.unofficial.proxertv.ui.util.extensions

import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Simpler [RequestBuilder.listener] interface.
 */
fun <T> RequestBuilder<T>.simpleListener(
    onLoadFailed: (GlideException?) -> Unit = { },
    onResourceReady: () -> Unit = { }
): RequestBuilder<T> {
    return listener(object : RequestListener<T> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<T>?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadFailed(e)
            return false
        }

        override fun onResourceReady(
            resource: T,
            model: Any?,
            target: Target<T>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onResourceReady()
            return false
        }
    })
}