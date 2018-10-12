package com.inverse.unofficial.proxertv.ui.util

import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

/**
 * A selector that uses the [LoadingCoverPresenter] for a [LoadingCover] and a [SeriesCoverPresenter] for everything
 * else.
 */
class CoverPresenterSelector(glide: GlideRequests) : PresenterSelector() {
    private val coverPresenter = SeriesCoverPresenter(glide)
    private val loadingPresenter = LoadingCoverPresenter()


    override fun getPresenter(item: Any): Presenter {
        if (item is LoadingCover) {
            return loadingPresenter
        }
        return coverPresenter
    }
}