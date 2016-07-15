package com.inverse.unofficial.proxertv.base

import android.app.Application
import com.inverse.unofficial.proxertv.base.client.ClientModule
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.db.MySeriesRepository
import com.inverse.unofficial.proxertv.base.db.StorageModule
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class BaseModule(val application: Application) {

    @Provides
    fun provideApplication(): Application {
        return application
    }
}

@Singleton
@Component(modules = arrayOf(BaseModule::class, ClientModule::class, StorageModule::class))
interface BaseComponent {
    fun getProxerClient(): ProxerClient
    fun getMySeriesRepository(): MySeriesRepository
}