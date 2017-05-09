package com.inverse.unofficial.proxertv.ui.login

import android.app.Activity
import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepFragment

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GuidedStepFragment.addAsRoot(this, LoginFragment(), android.R.id.content)
    }
}
