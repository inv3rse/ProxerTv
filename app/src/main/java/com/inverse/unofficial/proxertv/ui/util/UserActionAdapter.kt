package com.inverse.unofficial.proxertv.ui.util

import androidx.annotation.StringRes
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.inverse.unofficial.proxertv.R


/**
 * A possible user account action.
 */
enum class UserAction(@StringRes val textResource: Int) {
    LOGIN(R.string.user_action_login),
    LOGOUT(R.string.user_action_logout),
    SYNC(R.string.user_action_sync)
}

/**
 * A [UserAction] with loading information
 */
data class UserActionHolder(
        val userAction: UserAction,
        val isLoading: Boolean
)

/**
 * Presents a [UserActionHolder]
 */
class UserActionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return UserActionViewHolder(inflater.inflate(R.layout.view_user_action, parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val action = item as UserActionHolder
        val holder = viewHolder as UserActionViewHolder

        holder.textView.text = holder.textView.context.getText(action.userAction.textResource)
        holder.progressBar.visibility = if (action.isLoading) View.VISIBLE else View.GONE
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    /**
     * ViewHolder for a UserAction
     */
    internal class UserActionViewHolder(view: View) : Presenter.ViewHolder(view) {
        val textView = view.findViewById(R.id.user_action_text) as TextView
        val progressBar: View = view.findViewById(R.id.user_action_progress)
    }
}

/**
 * Adapter that shows a list of possible account actions (login, logout, sync)
 */
class UserActionAdapter(userLoggedIn: Boolean) : ObjectAdapter(UserActionPresenter()) {

    private val loadingSet = mutableSetOf<UserAction>()

    /**
     * Defines the possible user actions
     */
    var loggedIn = userLoggedIn
        set(value) {
            field = value
            notifyChanged()
        }

    override fun size(): Int {
        if (loggedIn) {
            return ACTIONS_LOGGED_IN.size
        } else {
            return ACTIONS_LOGGED_OUT.size
        }
    }

    override fun get(position: Int): Any {
        val action = if (loggedIn) ACTIONS_LOGGED_IN[position] else ACTIONS_LOGGED_OUT[position]
        return UserActionHolder(action, loadingSet.contains(action))
    }

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     * @param item the item to get the index for
     * @return the index or -1
     */
    fun indexOf(item: Any?): Int {
        if (item is UserActionHolder) {
            if (loggedIn) {
                return ACTIONS_LOGGED_IN.indexOf(item.userAction)
            } else {
                return ACTIONS_LOGGED_OUT.indexOf(item.userAction)
            }
        }
        return -1
    }

    /**
     * Sets the state of an action to loading
     * @param action the action to set to loading
     */
    fun addLoading(action: UserAction) {
        loadingSet.add(action)
        notifyChanged()
    }

    /**
     * Removes the loading state of an action
     * @param action the action
     */
    fun removeLoading(action: UserAction) {
        loadingSet.remove(action)
        notifyChanged()
    }

    companion object {
        private val ACTIONS_LOGGED_OUT = listOf(UserAction.LOGIN)
        private val ACTIONS_LOGGED_IN = listOf(UserAction.SYNC, UserAction.LOGOUT)
    }
}