package com.inverse.unofficial.proxertv.base.client.util

/**
 * CloudFlare related constants.
 */
@Suppress("MagicNumber")
object CloudFlareConstants {
    const val SERVER_NAME = "cloudflare"

    private val CAPTCHA_RESPONSE_CODES = listOf(403)
    val BROWSER_CHALLENGE_RESPONSE_CODES = listOf(429, 503)
    val RESPONSE_CODES = CAPTCHA_RESPONSE_CODES + BROWSER_CHALLENGE_RESPONSE_CODES
}
