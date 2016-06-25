package com.inverse.unofficial.proxertv.ui.util

import android.content.Context
import android.os.Bundle
import android.view.View
import com.inverse.unofficial.proxertv.R
import org.jetbrains.anko.withArguments

class ErrorFragment : android.support.v17.leanback.app.ErrorFragment(), View.OnClickListener {
    var dismissListener: ErrorDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments.getString(KEY_TITLE)
        message = arguments.getString(KEY_MSG)
        setDefaultBackground(false)

        buttonText = getString(R.string.dismiss_error)
        buttonClickListener = this
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val drawableId = android.support.v17.leanback.R.drawable.lb_ic_sad_cloud
        imageDrawable = resources.getDrawable(drawableId, context?.theme)
    }

    override fun onClick(p0: View?) {
        dismissListener?.onDismiss()
    }

    interface ErrorDismissListener {
        fun onDismiss()
    }

    companion object {
        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MSG = "KEY_MSG"

        fun newInstance(title: String, message: String): ErrorFragment {
            return ErrorFragment().withArguments(
                    KEY_TITLE to title,
                    KEY_MSG to message)
        }
    }
}