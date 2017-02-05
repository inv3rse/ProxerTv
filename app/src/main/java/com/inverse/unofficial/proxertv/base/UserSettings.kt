package com.inverse.unofficial.proxertv.base

import android.content.SharedPreferences
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject

abstract class UserSettings {

    private val changeSubject = SerializedSubject(PublishSubject.create<User>())

    /**
     * Observe changes to the user account
     */
    fun observeAccount(): Observable<User> {
        return changeSubject
    }

    /**
     * Set username and password.
     * @param username the username
     * @param password the password
     */
    fun setAccount(username: String, password: String) {
        setUser(username, password)
        notifyChange()
    }

    /**
     * Clears user name, password and token
     */
    fun clearUser() {
        setUser(null, null)
        setUserToken(null)
        notifyChange()
    }

    private fun notifyChange() {
        changeSubject.onNext(getUser())
    }

    /**
     * Set username and password
     * @param username the username
     * @param password the password
     */
    protected abstract fun setUser(username: String?, password: String?)

    /**
     * Set the user token
     * @param userToken the user token
     */
    abstract fun setUserToken(userToken: String?)

    /**
     * Get the {@link User} object
     * @return the current user
     */
    abstract fun getUser(): User?

    /**
     * Get the current user token
     * @return the user token
     */
    abstract fun getUserToken(): String?
}


/**
 * User settings for storing name password and token
 */
class UserSettingsPrefs(private val prefs: SharedPreferences) : UserSettings() {

    override fun setUser(username: String?, password: String?) {
        prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
    }

    override fun setUserToken(userToken: String?) {
        prefs.edit().putString(KEY_USER_TOKEN, userToken).apply()
    }

    override fun getUser(): User? {
        val username = prefs.getString(KEY_USERNAME, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (username != null && password != null) {
            return User(username, password, prefs.getString(KEY_USER_TOKEN, null))
        }

        return null
    }

    override fun getUserToken(): String? {
        return prefs.getString(KEY_USER_TOKEN, null)
    }

    companion object {
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_PASSWORD = "KEY_PASSWORD"
        private const val KEY_USER_TOKEN = "KEY_USER_TOKEN"
    }
}

/**
 * Data class representing a user
 */
data class User(
        val username: String,
        val password: String,
        val userToken: String?
)