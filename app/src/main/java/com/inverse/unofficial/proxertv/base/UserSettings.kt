package com.inverse.unofficial.proxertv.base

import android.content.SharedPreferences

class UserSettings(private val prefs: SharedPreferences) {

    fun setUser(username: String?, password: String?) {
        prefs.edit().putString(KEY_USERNAME, username).putString(KEY_PASSWORD, password).apply()
    }

    fun setUserToken(userToken: String?) {
        prefs.edit().putString(KEY_USER_TOKEN, userToken).apply()
    }

    fun getUser(): User? {
        val username = prefs.getString(KEY_USERNAME, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (username != null && password != null) {
            return User(username, password, prefs.getString(KEY_USER_TOKEN, null))
        }

        return null
    }

    fun clearUser() {
        setUser(null, null)
        setUserToken(null)
    }

    fun getUserToken(): String? {
        return prefs.getString(KEY_USER_TOKEN, null)
    }

    companion object {
        const val KEY_USERNAME = "KEY_USERNAME"
        const val KEY_PASSWORD = "KEY_PASSWORD"
        const val KEY_USER_TOKEN = "KEY_USER_TOKEN"
    }
}

data class User(
        val username: String,
        val password: String,
        val userToken: String?
)