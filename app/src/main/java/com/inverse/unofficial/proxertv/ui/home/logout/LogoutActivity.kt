package com.inverse.unofficial.proxertv.ui.home.logout

import android.app.Activity
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepFragment

/**
 * Activity showing the [LogoutFragment]
 */
class LogoutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuidedStepFragment.addAsRoot(this, LogoutFragment(), android.R.id.content)
    }
}