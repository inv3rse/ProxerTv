package com.inverse.unofficial.proxertv.ui.details

import android.app.Activity
import android.os.Bundle
import com.inverse.unofficial.proxertv.R

class DetailsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
    }

    companion object {
        const val EXTRA_SERIES_ID = "ARG_SERIES_ID"
        const val SHARED_ELEMENT_COVER = "series_cover"
    }
}