package com.inverse.unofficial.proxertv.base

import com.google.firebase.crash.FirebaseCrash
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
            FirebaseCrash.report(error)
        } else {
            error.printStackTrace()
        }
    }
}