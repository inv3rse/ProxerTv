package com.inverse.unofficial.proxertv.base

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.inverse.unofficial.proxertv.BuildConfig
import io.fabric.sdk.android.Fabric
import timber.log.Timber
import java.io.IOException

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Fabric.with(this, Crashlytics())
        }

        CrashReporting.logExeptionForRelease(IOException("test error"))
        component = DaggerBaseComponent.builder().baseModule(BaseModule(this)).build()
    }

    companion object {
        lateinit var component: BaseComponent
            private set
    }
}