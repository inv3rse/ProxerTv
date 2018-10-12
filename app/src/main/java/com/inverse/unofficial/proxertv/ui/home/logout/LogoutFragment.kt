package com.inverse.unofficial.proxertv.ui.home.logout

import android.os.Bundle
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import org.jetbrains.anko.toast
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Fragment handling the user logout
 */
class LogoutFragment : GuidedStepSupportFragment() {

    private val repository = App.component.getProxerRepository()
    private var logoutPending = false

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {

        val title = getString(R.string.logout_title)
        val description = getString(R.string.logout_description)

        return GuidanceStylist.Guidance(title, description, "", null)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(GuidedAction.Builder(activity)
                .id(LOGOUT_ID)
                .title(R.string.logout_confirm)
                .build())

        actions.add(GuidedAction.Builder(activity)
                .id(CANCEL_ID)
                .title(R.string.logout_cancel)
                .build())
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            LOGOUT_ID -> {
                if (!logoutPending) {
                    logoutPending = true
                    repository.logout()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    // on success
                                    {
                                        finishGuidedStepSupportFragments()
                                    },
                                    // on error
                                    {
                                        CrashReporting.logException(it)
                                        requireContext().toast(R.string.logout_error)
                                        logoutPending = false
                                    })
                }
            }
            CANCEL_ID -> finishGuidedStepSupportFragments()
        }
    }

    companion object {
        private const val LOGOUT_ID = 1L
        private const val CANCEL_ID = 2L
    }
}