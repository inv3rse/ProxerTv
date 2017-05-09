package com.inverse.unofficial.proxertv.ui.player

import android.support.v17.leanback.R
import android.support.v17.leanback.widget.Action
import android.support.v17.leanback.widget.ControlButtonPresenterSelector
import android.support.v17.leanback.widget.PlaybackControlsRow
import android.support.v17.leanback.widget.Presenter
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.TextView

/**
 * Copy of a [ControlButtonPresenterSelector] (25.2.0) that does not show actions with no id.
 * The invisible action in the layout is necessary because the [PlaybackControlsRow] puts the default focus on the
 * middle item based on child count.
 *
 * The [ControlButtonPresenterSelector] is a required superclass because the [PlaybackControlsRow] checks via instance of.
 */
class ControlButtonPresenterSelector42 : ControlButtonPresenterSelector() {

    private val primaryPresenter = ControlButtonPresenter(R.layout.lb_control_button_primary)

    override fun getPrimaryPresenter(): Presenter {
        return primaryPresenter
    }

    internal class ActionViewHolder(view: View) : Presenter.ViewHolder(view) {
        var mIcon: ImageView = view.findViewById(R.id.icon) as ImageView
        var mLabel: TextView? = view.findViewById(R.id.label) as TextView
        var mFocusableView: View = view.findViewById(R.id.button)
    }

    internal class ControlButtonPresenter(private val mLayoutResourceId: Int) : Presenter() {

        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val v = LayoutInflater.from(parent.context)
                    .inflate(mLayoutResourceId, parent, false)
            return ActionViewHolder(v)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            val action = item as Action
            val vh = viewHolder as ActionViewHolder

            if (action.id == Action.NO_ID) {
                vh.view.visibility = View.GONE
            } else {
                vh.view.visibility = View.VISIBLE

                vh.mIcon.setImageDrawable(action.icon)
                if (vh.mLabel != null) {
                    if (action.icon == null) {
                        vh.mLabel!!.text = action.label1
                    } else {
                        vh.mLabel!!.text = null
                    }
                }
                val contentDescription = if (TextUtils.isEmpty(action.label2))
                    action.label1
                else
                    action.label2
                if (!TextUtils.equals(vh.mFocusableView.contentDescription, contentDescription)) {
                    vh.mFocusableView.contentDescription = contentDescription
                    vh.mFocusableView.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
                }
            }
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
            val vh = viewHolder as ActionViewHolder
            vh.mIcon.setImageDrawable(null)
            if (vh.mLabel != null) {
                vh.mLabel!!.text = null
            }
            vh.mFocusableView.contentDescription = null
        }

        override fun setOnClickListener(viewHolder: Presenter.ViewHolder,
                                        listener: View.OnClickListener) {
            (viewHolder as ActionViewHolder).mFocusableView.setOnClickListener(listener)
        }
    }
}
