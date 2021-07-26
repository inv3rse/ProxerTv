package com.inverse.unofficial.proxertv.base.client.util

import android.app.Application
import android.webkit.CookieManager
import androidx.annotation.MainThread
import dagger.Reusable
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject

/**
 * A [CookieJar] implementation that uses the WebViews [CookieManager] internally.
 * Effectively allow OkHttp to share its cookies with the WebView.
 */
@Reusable
class AndroidWebViewCookieJar @Inject constructor(app: Application) : CookieJar {

    private val manager = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val urlString = url.toString()

        for (cookie in cookies) {
            manager.setCookie(urlString, cookie.toString())
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())
        return if (cookies != null && cookies.isNotEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else {
            emptyList()
        }
    }

    /**
     * Clear all cookies.
     */
    @MainThread
    fun clear() {
        manager.removeAllCookies {}
    }
}
