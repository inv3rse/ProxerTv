package com.inverse.unofficial.proxertv.base

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.inverse.unofficial.proxertv.BuildConfig
import com.inverse.unofficial.proxertv.base.utils.CrashlyticsTree
import io.fabric.sdk.android.Fabric
import timber.log.Timber

/**
 * The application
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())

        Timber.plant(CrashlyticsTree())

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        component = DaggerBaseComponent.builder().baseModule(BaseModule(this)).build()
    }

    companion object {
        lateinit var component: BaseComponent
            private set
    }
}