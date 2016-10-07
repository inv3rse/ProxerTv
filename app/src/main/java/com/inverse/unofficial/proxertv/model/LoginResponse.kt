package com.inverse.unofficial.proxertv.model

/**
 * The api response of an successful login
 */
data class LoginResponse(
        val uid: String,
        val avatar: String,
        val token: String
)