package com.inverse.unofficial.proxertv.base

import android.app.Application
import android.content.Context
import com.inverse.unofficial.proxertv.base.client.ClientModule
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.db.StorageModule
import com.inverse.unofficial.proxertv.ui.details.DetailsViewModel
import com.inverse.unofficial.proxertv.ui.home.HomeViewModel
import com.inverse.unofficial.proxertv.ui.player.PlayerViewModel
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import javax.inject.Qualifier
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
    @IODispatcher
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class IODispatcher

@Singleton
@Component(modules = [BaseModule::class, ClientModule::class, StorageModule::class])
interface BaseComponent {
    fun getUserSettings(): UserSettings
    fun getProxerClient(): ProxerClient
    fun getProxerRepository(): ProxerRepository

    @ClientModule.GlideHttpClient
    fun getGlideHttpClient(): OkHttpClient

    fun getHomeViewModel(): HomeViewModel
    fun getDetailsViewModel(): DetailsViewModel
    fun getPlayerViewModel(): PlayerViewModel
}