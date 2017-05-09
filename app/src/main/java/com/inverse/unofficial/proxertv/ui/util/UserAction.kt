package com.inverse.unofficial.proxertv.ui.util

import android.support.annotation.StringRes
import com.inverse.unofficial.proxertv.R

/**
 * A possible user account action.
 */
enum class UserAction(@StringRes val textResource: Int) {
    LOGIN(R.string.user_action_login),
    LOGOUT(R.string.user_action_logout),
    SYNC(R.string.user_action_sync)
}