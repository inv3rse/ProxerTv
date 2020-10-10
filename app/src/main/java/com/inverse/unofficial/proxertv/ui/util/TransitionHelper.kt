package com.inverse.unofficial.proxertv.ui.util

import android.view.View
import androidx.core.view.ViewCompat

/**
 * Helper for activity transitions
 */
object TransitionHelper {

    /**
     * Get the transition name for the series cover
     */
    fun getCoverTransitionName(seriesId: Int) = "cover_$seriesId"

    /**
     * Set the transition name for the series cover
     */
    fun setCoverTransitionName(view: View, seriesId: Int) {
        ViewCompat.setTransitionName(view, getCoverTransitionName(seriesId))
    }
}