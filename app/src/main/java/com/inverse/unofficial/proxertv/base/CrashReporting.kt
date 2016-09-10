package com.inverse.unofficial.proxertv.base

import com.crashlytics.android.BuildConfig
import com.crashlytics.android.Crashlytics

/**
 * Util object to log errors
 */
object CrashReporting {

    /**
     * Log the exception
     */
    fun logException(error: Throwable) {
        Crashlytics.logException(error)
        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
    }
}