package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.ObjectAdapter
import com.inverse.unofficial.proxertv.base.UserSettings

/**
 * Adapter that shows a list of possible account actions (login, logout, sync)
 */
class UserActionAdapter(private val userSettings: UserSettings) : ObjectAdapter(UserActionPresenter()) {

    /**
     * Notifies that the account has changed
     */
    fun notifyAccountChanged() {
        notifyChanged()
    }

    override fun size(): Int {
        if (userSettings.getUser() != null) {
            return ACTIONS_LOGGED_IN.size
        } else {
            return ACTIONS_LOGGED_OUT.size
        }
    }

    override fun get(position: Int): Any {
        if (userSettings.getUser() != null) {
            return ACTIONS_LOGGED_IN[position]
        } else {
            return ACTIONS_LOGGED_OUT[position]
        }
    }

    companion object {
        private val ACTIONS_LOGGED_OUT = listOf(UserAction.LOGIN)
        private val ACTIONS_LOGGED_IN = listOf(UserAction.SYNC, UserAction.LOGOUT)
    }
}