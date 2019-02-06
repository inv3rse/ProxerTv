package com.inverse.unofficial.proxertv.ui.util

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.leanback.app.ErrorSupportFragment
import com.inverse.unofficial.proxertv.R

class ErrorFragment : ErrorSupportFragment(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(KEY_TITLE)
        message = arguments?.getString(KEY_MSG)
        setDefaultBackground(false)

        buttonText = getString(R.string.dismiss_error)
        buttonClickListener = this
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val drawableId = androidx.leanback.R.drawable.lb_ic_sad_cloud
        imageDrawable = resources.getDrawable(drawableId, context?.theme)
    }

    override fun onClick(p0: View?) {
        when {
            targetFragment != null -> {
                (targetFragment as? EventListener)?.onDismissError()
            }
            else -> {
                (activity as? EventListener)?.onDismissError()
            }
        }
    }

    interface EventListener {
        fun onDismissError()
    }

    companion object {
        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_MSG = "KEY_MSG"

        fun newInstance(title: String, message: String?): ErrorFragment {
            val arguments = Bundle()
            arguments.putString(KEY_TITLE, title)
            arguments.putString(KEY_MSG, message)

            val fragment = ErrorFragment()
            fragment.arguments = arguments
            return fragment
        }
    }
}