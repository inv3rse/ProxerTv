package com.inverse.unofficial.proxertv.ui.util

import androidx.annotation.StringRes
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.model.SeriesList

@StringRes
fun SeriesList.getStringRes(): Int {
    return when (this) {
        SeriesList.NONE -> R.string.series_list_none
        SeriesList.ABORTED -> R.string.series_list_aborted
        SeriesList.WATCHLIST -> R.string.series_list_watchlist
        SeriesList.FINISHED -> R.string.series_list_finished
    }
}