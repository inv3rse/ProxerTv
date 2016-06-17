package com.inverse.unofficial.proxertv.base

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        component = DaggerBaseComponent.builder().baseModule(BaseModule(this)).build()
    }

    companion object {
        lateinit var component: BaseComponent
            private set
    }
}