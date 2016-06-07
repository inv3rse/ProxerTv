package com.example.dennis.proxertv.ui.details

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter
import com.example.dennis.proxertv.model.Series

class DetailsDescriptionPresenter: AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(vh: ViewHolder, item: Any?) {
        if (item is Series) {
            vh.title.text = item.englishTitle
            vh.subtitle.text = item.originalTitle
            vh.body.text = item.description
        }
    }
}