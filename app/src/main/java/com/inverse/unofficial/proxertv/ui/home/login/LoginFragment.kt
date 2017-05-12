package com.inverse.unofficial.proxertv.ui.home.login

import android.os.Bundle
import android.support.v17.leanback.app.GuidedStepFragment
import android.support.v17.leanback.widget.GuidanceStylist
import android.support.v17.leanback.widget.GuidedAction
import android.text.InputType
import android.widget.Toast
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.base.ProxerRepository
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Fragment handling the user login.
 */
class LoginFragment : GuidedStepFragment() {

    private lateinit var repository: ProxerRepository
    private var loginSubscription: Subscription? = null

    private var username: String = ""
    private var password: String = ""

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = App.component.getProxerRepository()
    }

    /**
     * Returns the information required to provide guidance to the user. This hook is called during
     * {@link #onCreateView}.  May be overridden to return a custom subclass of {@link
     * GuidanceStylist.Guidance} for use in a subclass of {@link GuidanceStylist}. The default
     * returns a Guidance object with empty fields; subclasses should override.
     *
     * @param savedInstanceState The saved instance state from onCreateView.
     * @return The Guidance object representing the information used to guide the user.
     */
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {

        val title = getString(R.string.login_title)
        val description = getString(R.string.login_description)

        return GuidanceStylist.Guidance(title, description, "", null)
    }

    /**
     * Fills out the set of actions available to the user. This hook is called during {@link
     * #onCreate}. The default leaves the list of actions empty; subclasses should override.
     *
     * @param actions A non-null, empty list ready to be populated.
     * @param savedInstanceState The saved instance state from onCreate.
     */
    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        actions.add(GuidedAction.Builder(activity)
                .id(USERNAME_ID)
                .editable(true)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .editInputType(InputType.TYPE_CLASS_TEXT)
                .description(getString(R.string.login_username_title))
                .build())

        actions.add(GuidedAction.Builder(activity)
                .id(PASSWORD_ID)
                .editable(true)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .editInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .description(getString(R.string.login_password_title))
                .build())

        actions.add(GuidedAction.Builder(activity)
                .id(LOGIN_ID)
                .title(getString(R.string.login_action_title))
                .hasNext(true)
                .build())
    }

    /**
     * Callback invoked when an action has been edited, for example when user clicks confirm button
     * in IME window.  Default implementation calls deprecated method
     * [.onGuidedActionEdited] and returns [GuidedAction.ACTION_ID_NEXT].

     * @param action The action that has been edited.
     * *
     * @return ID of the action will be focused or [GuidedAction.ACTION_ID_NEXT],
     * * [GuidedAction.ACTION_ID_CURRENT].
     */
    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        when (action.id) {
            USERNAME_ID -> username = action.title.toString()
            PASSWORD_ID -> password = action.title.toString()
        }

        return super.onGuidedActionEditedAndProceed(action)
    }

    /**
     * Callback invoked when an action has been canceled editing, for example when user closes
     * IME window by BACK key.  Default implementation calls deprecated method
     * [.onGuidedActionEdited].
     * @param action The action which has been canceled editing.
     */
    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        when (action.id) {
            USERNAME_ID -> username = action.title.toString()
            PASSWORD_ID -> password = action.title.toString()
        }
        super.onGuidedActionEditCanceled(action)
    }

    /**
     * Callback invoked when an action is taken by the user. Subclasses should override in
     * order to act on the user's decisions.
     *
     * @param action The chosen action.
     */
    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == LOGIN_ID) {
            loginSubscription?.unsubscribe()
            if (validateInput()) {
                loginSubscription = repository.login(username, password)
                        .flatMap { repository.syncUserList(true) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                // on next
                                { _ ->
                                    finishGuidedStepFragments()
                                },
                                // on error
                                { error ->
                                    if (error is ApiErrorException) {
                                        showErrorMsg(error.msg ?: getString(R.string.login_error_general))
                                    } else {
                                        CrashReporting.logException(error)
                                        showErrorMsg(getString(R.string.login_error_general))
                                    }
                                }
                        )
            }
        }
    }

    private fun validateInput(): Boolean {
        when {
            username.isBlank() -> showErrorMsg(getString(R.string.login_error_username_empty))
            password.isBlank() -> showErrorMsg(getString(R.string.login_error_password_empty))
            else -> return true
        }

        return false
    }

    private fun showErrorMsg(msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val USERNAME_ID = 1L
        const val PASSWORD_ID = 2L
        const val LOGIN_ID = 3L
    }
}













