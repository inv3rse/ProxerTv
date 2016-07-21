package com.inverse.unofficial.proxertv.ui.details

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter
import com.inverse.unofficial.proxertv.model.Series

class DetailsDescriptionPresenter: AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(vh: ViewHolder, item: Any?) {
        if (item is Series) {
            vh.title.text = item.name
            vh.body.text = item.description
        }
    }
}