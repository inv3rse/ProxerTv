package com.inverse.unofficial.proxertv.ui.home.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment

/**
 * Activity showing a [LoginFragment]
 */
class LoginActivity : FragmentActivity() {

    companion object {
        /**
         * Create an [Intent] to the [LoginActivity]
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuidedStepSupportFragment.addAsRoot(this, LoginFragment(), android.R.id.content)
    }
}
