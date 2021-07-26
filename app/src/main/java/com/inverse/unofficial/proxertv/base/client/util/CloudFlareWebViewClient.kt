package com.inverse.unofficial.proxertv.base.client.util

import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * A [WebViewClient] that tries to detect CloudFlare related events.
 */
class CloudFlareWebViewClient(
    private val cookieJar: AndroidWebViewCookieJar,
    private val targetUrl: String,
    private val challengeListener: ChallengeResultListener
) : WebViewClient() {

    private val oldCookie = cookieJar.getCfClearanceCookie(targetUrl)
    private var webChallengeDetected = false

    private var done = false

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (done) return

        if (hasClearanceCookie()) {
            challengeListener.clearanceCookieUpdated()
            done = true
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (done) return

        if (hasClearanceCookie()) {
            challengeListener.clearanceCookieUpdated()
            done = true
            return
        }

        if (url != targetUrl) return

        // Check if a challenge is displayed on the page. Otherwise something went wrong if there is no challenge and
        // no cookie. For API 23 and upwards we can just check the http response code. Below that we just hope for the
        // best.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!webChallengeDetected) {
                challengeListener.noChallengeDetected()
                done = true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (done) return

        if (request.isForMainFrame) {
            if (errorResponse.statusCode in CloudFlareConstants.RESPONSE_CODES) {
                webChallengeDetected = true
            }
        }
    }

    private fun hasClearanceCookie(): Boolean {
        return cookieJar.getCfClearanceCookie(targetUrl).let { it != null && it != oldCookie }
    }

    /**
     * Listener for the result of CloudFlare challenges.
     */
    interface ChallengeResultListener {

        /**
         * The challenge has been solved and a new clearance cookie has been saved.
         */
        fun clearanceCookieUpdated()

        /**
         * Failed to detect a challenge on the website.
         */
        fun noChallengeDetected()
    }
}
