package com.inverse.unofficial.proxertv.base.client.util

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebSettings
import android.webkit.WebView
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * [Interceptor] that masks the request as if were coming from a WebView.
 * Tries to solve CloudFlare browser checks in a headless [WebView].
 * Captcha checks will be directly issued to the [cloudFlareInteractionManager].
 *
 * Relies on the [cookieJar] sharing it's values with the [WebView].
 */
class CloudFlareInterceptor @Inject constructor(
    private val context: Application,
    private val cookieJar: AndroidWebViewCookieJar,
) : Interceptor {

    private val userAgent by lazy { WebSettings.getDefaultUserAgent(context) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val browserRequest = originalRequest.newBuilder()
            .impersonateWebView()
            .build()

        val response = chain.proceed(browserRequest)

        if (response.header("Server")?.contains(CloudFlareConstants.SERVER_NAME) == true
            && response.code in CloudFlareConstants.RESPONSE_CODES
        ) {
            // maybe try to get the wait time here
            response.close()

            if (response.code in CloudFlareConstants.BROWSER_CHALLENGE_RESPONSE_CODES) {
                try {
                    val oldClearance = cookieJar.getCfClearanceCookie(originalRequest)
                    solveChallengeWithHeadlessWebView(browserRequest, oldClearance)
                    return chain.proceed(browserRequest)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }

            //cloudFlareInteractionManager.issueChallenge(originalRequest.url.toString())
            throw UserInteractionRequiredException()
        }

        return response
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Synchronized
    private fun solveChallengeWithHeadlessWebView(request: Request, oldClearance: Cookie?) {
        // check if another request updated the cookie while we were blocked (@Synchronized)
        val existingClearance = cookieJar.getCfClearanceCookie(request)
        if (existingClearance != null && existingClearance != oldClearance) {
            return
        }

        val targetUrl = request.url.toString()

        val clearanceUpdated = runBlocking(Dispatchers.Main) {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
            }

            try {
                // Wait for the WebView to solve the Javascript challenge, but only for a reasonable time.
                withTimeout(TimeUnit.SECONDS.toMillis(10)) {
                    suspendCancellableCoroutine { continuation: CancellableContinuation<Boolean> ->
                        val cloudFlareClient = CloudFlareWebViewClient(cookieJar, targetUrl,
                            object : CloudFlareWebViewClient.ChallengeResultListener {
                                override fun clearanceCookieUpdated() {
                                    continuation.resume(true)
                                }

                                override fun noChallengeDetected() {
                                    continuation.resume(false)
                                }
                            })

                        webView.apply {
                            webViewClient = cloudFlareClient
                            loadUrl(targetUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                false
            } finally {
                webView.stopLoading()
                webView.destroy()
            }
        }

        if (!clearanceUpdated) {
            throw HeadlessSolveException()
        }
    }

    private fun Request.Builder.impersonateWebView(): Request.Builder {
        header("User-Agent", userAgent)
        header("Accept", ACCEPT_CONTENT)
        header("Accept-Language", ACCEPT_LANGUAGES)
        // Not clear why, but the automatic transparent addition of the gzip encoding does not work reliable.
        header("Accept-Encoding", "gzip, br")
        header("Upgrade-Insecure-Requests", "1")
        return this
    }

    /**
     * Exception that is thrown if user interaction is required to solve the CloudFlare challenge.
     * [CloudFlareInteractionManager.issueChallenge] will be invoked.
     */
    class UserInteractionRequiredException : IOException()

    /**
     * Failed to solve the challenge with a headless WebView.
     */
    private class HeadlessSolveException : IOException()

    companion object {
        private const val ACCEPT_LANGUAGES = "de,en-US;q=0.7,en;q=0.3"
        private const val ACCEPT_CONTENT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
    }

}
