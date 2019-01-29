package com.inverse.unofficial.proxertv.base.utils

import okhttp3.internal.http2.StreamResetException
import java.io.EOFException
import java.net.*
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

private const val CHECK_DEPTH = 4

/**
 * Checks if the exception is or was caused by a network error.
 */
fun Throwable.isNetworkException(): Boolean {
    return generateSequence(this) { if (it != it.cause) it.cause else null }
        .take(CHECK_DEPTH)
        .any { isNetworkExceptionActual(it) }
}

private fun isNetworkExceptionActual(t: Throwable?): Boolean {
    if (t == null) {
        return false
    }

    return t is UnknownHostException || t is SocketTimeoutException || t is SocketException || t is EOFException
            || t is ConnectException || t is NoRouteToHostException || t is SSLHandshakeException
            || t is SSLProtocolException || t is StreamResetException
}