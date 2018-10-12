package com.inverse.unofficial.proxertv.ui.home.logout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

/**
 * Activity showing the [LogoutFragment]
 */
class LogoutActivity : FragmentActivity() {

    companion object {
        /**
         * Create an [Intent] to the [LogoutActivity]
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, LogoutActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuidedStepSupportFragment.addAsRoot(this, LogoutFragment(), android.R.id.content)
    }
}