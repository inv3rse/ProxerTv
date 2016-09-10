package com.inverse.unofficial.proxertv.base

import com.crashlytics.android.Crashlytics
import com.inverse.unofficial.proxertv.BuildConfig

/**
 * Util object to log errors
 */
object CrashReporting {

    /**
     * Log the exception if we are running a release build
     */
    fun logExeptionForRelease(error: Throwable) {
        if (!BuildConfig.DEBUG) {
            Crashlytics.logException(error)
        } else {
            error.printStackTrace()
        }
    }
}