package com.inverse.unofficial.proxertv.base

/**
 * In memory implementation of the [UserSettings] for testing
 */
class UserSettingsMemory : UserSettings {
    private var username: String? = null
    private var password: String? = null
    private var userToken: String? = null

    override fun setUser(username: String?, password: String?) {
        this.username = username
        this.password = password
    }

    override fun setUserToken(userToken: String?) {
        this.userToken = userToken
    }

    override fun getUser(): User? {
        if (username != null && password != null) {
            return User(username!!, password!!, userToken)
        }

        return null
    }

    override fun getUserToken(): String? {
        return userToken
    }

    override fun clearUser() {
        username = null
        password = null
        userToken = null
    }
}