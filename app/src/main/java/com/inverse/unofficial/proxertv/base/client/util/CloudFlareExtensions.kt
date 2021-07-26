package com.inverse.unofficial.proxertv.base.client.util

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * Load the CloudFlare clearance cookie for the given [url].
 */
fun CookieJar.getCfClearanceCookie(url: String): Cookie? {
    return url.toHttpUrlOrNull()?.let { getCfClearanceCookie(it) }
}

/**
 * Load the CloudFlare clearance cookie for the given [request].
 */
fun CookieJar.getCfClearanceCookie(request: Request): Cookie? {
    return getCfClearanceCookie(request.url)
}

/**
 * Load the CloudFlare clearance cookie for the given [url].
 */
fun CookieJar.getCfClearanceCookie(url: HttpUrl): Cookie? {
    return loadForRequest(url).firstOrNull { it.name == "cf_clearance" }
}
