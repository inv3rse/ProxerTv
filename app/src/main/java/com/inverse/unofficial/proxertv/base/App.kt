package com.inverse.unofficial.proxertv.base

import android.app.Application
import com.inverse.unofficial.proxertv.BuildConfig
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
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