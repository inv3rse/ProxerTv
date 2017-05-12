package com.inverse.unofficial.proxertv.ui.home.login

import android.app.Activity
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepFragment

/**
 * Activity showing a [LoginFragment]
 */
class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuidedStepFragment.addAsRoot(this, LoginFragment(), android.R.id.content)
    }
}
