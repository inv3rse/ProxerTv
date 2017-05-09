package com.inverse.unofficial.proxertv.ui.util

import android.support.v17.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.inverse.unofficial.proxertv.R

/**
 * Presents a [UserAction]
 */
class UserActionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return UserActionViewHolder(inflater.inflate(R.layout.view_user_action, parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val action = item as UserAction
        val holder = viewHolder as UserActionViewHolder

        holder.textView.text = holder.textView.context.getText(action.textResource)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
    }

    /**
     * ViewHolder for a UserAction
     */
    internal class UserActionViewHolder(view: View) : Presenter.ViewHolder(view) {
        val imageView = view.findViewById(R.id.user_action_image) as ImageView
        val textView = view.findViewById(R.id.user_action_text) as TextView
    }
}