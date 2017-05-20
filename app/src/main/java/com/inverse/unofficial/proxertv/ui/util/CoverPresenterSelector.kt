package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.Presenter
import android.support.v17.leanback.widget.PresenterSelector

/**
 * A selector that uses the [LoadingCoverPresenter] for a [LoadingCover] and a [SeriesCoverPresenter] for everything
 * else.
 */
class CoverPresenterSelector : PresenterSelector() {
    private val coverPresenter = SeriesCoverPresenter()
    private val loadingPresenter = LoadingCoverPresenter()


    override fun getPresenter(item: Any): Presenter {
        if (item is LoadingCover) {
            return loadingPresenter
        }
        return coverPresenter
    }
}