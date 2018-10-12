package com.inverse.unofficial.proxertv.ui.details

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.inverse.unofficial.proxertv.R

class DetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
    }

    companion object {
        const val EXTRA_SERIES_ID = "ARG_SERIES_ID"
        const val SHARED_ELEMENT_COVER = "series_cover"
    }
}