package com.inverse.unofficial.proxertv.base

import android.app.Application
import android.content.Context
import com.inverse.unofficial.proxertv.base.client.ClientModule
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
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

    @Provides
    @Singleton
    fun provideUserSettings(application: Application): UserSettings {
        val prefs = application.getSharedPreferences("userSettings", Context.MODE_PRIVATE)
        return UserSettingsPrefs(prefs)
    }

    @Provides
    @Singleton
    fun provideProxerRepository(
            client: ProxerClient,
            mySeriesDb: MySeriesDb,
            progressDb: SeriesProgressDb,
            userSettings: UserSettings): ProxerRepository {

        return ProxerRepository(client, mySeriesDb, progressDb, userSettings)
    }
}

@Singleton
@Component(modules = arrayOf(BaseModule::class, ClientModule::class, StorageModule::class))
interface BaseComponent {
    fun getUserSettings(): UserSettings
    fun getProxerClient(): ProxerClient
    fun getProxerRepository(): ProxerRepository
}