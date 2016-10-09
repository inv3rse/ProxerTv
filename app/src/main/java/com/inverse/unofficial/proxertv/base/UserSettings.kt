package com.inverse.unofficial.proxertv.base

import android.content.SharedPreferences

/**
 * User settings for storing name password and token
 */
class UserSettings(private val prefs: SharedPreferences) {

    /**
     * Set username and password
     * @param username the username
     * @param password the password
     */
    fun setUser(username: String?, password: String?) {
        prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
    }

    /**
     * Set the user token
     * @param userToken the user token
     */
    fun setUserToken(userToken: String?) {
        prefs.edit().putString(KEY_USER_TOKEN, userToken).apply()
    }

    /**
     * Get the {@link User} object
     * @return the current user
     */
    fun getUser(): User? {
        val username = prefs.getString(KEY_USERNAME, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (username != null && password != null) {
            return User(username, password, prefs.getString(KEY_USER_TOKEN, null))
        }

        return null
    }

    /**
     * Clears user name, password and token
     */
    fun clearUser() {
        setUser(null, null)
        setUserToken(null)
    }

    /**
     * Get the current user token
     * @return the user token
     */
    fun getUserToken(): String? {
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