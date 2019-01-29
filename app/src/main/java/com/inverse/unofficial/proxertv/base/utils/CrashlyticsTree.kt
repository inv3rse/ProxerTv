package com.inverse.unofficial.proxertv.base.utils

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * A [Timber.Tree] that logs to Crashlytics
 */
class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority < Log.INFO) {
            return
        }

        Crashlytics.log(priority, tag, message)
        if (throwable != null && !throwable.isNetworkException()) {
            Crashlytics.logException(throwable)
        }
    }
}
