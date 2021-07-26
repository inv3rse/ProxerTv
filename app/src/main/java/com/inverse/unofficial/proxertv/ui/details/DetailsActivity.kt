package com.inverse.unofficial.proxertv.ui.details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.ui.util.TransitionHelper

/**
 * Details page for a series. Uses the [SeriesDetailsFragment].
 */
class DetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
    }

    companion object {
        const val EXTRA_SERIES_ID = "ARG_SERIES_ID"

        /**
         * Create an [Intent] with [ActivityOptionsCompat] for the transition to the [DetailsActivity]
         */
        fun createIntentWithOptions(
            activity: Activity,
            seriesId: Long,
            coverView: View
        ): Pair<Intent, ActivityOptionsCompat> {
            val intent = Intent(activity, DetailsActivity::class.java)
            intent.putExtra(EXTRA_SERIES_ID, seriesId)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                coverView,
                TransitionHelper.getCoverTransitionName(seriesId)
            )

            return Pair(intent, options)
        }
    }
}