package com.inverse.unofficial.proxertv.base

import android.content.Context
import com.google.gson.Gson
import com.inverse.unofficial.proxertv.BuildConfig
import com.inverse.unofficial.proxertv.model.ServerConfig
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class BaseModule(val applicationContext: Context) {

    @Provides
    fun provideApplicationContext(): Context {
        return applicationContext
    }

    @Provides
    fun provideOkHttpClient(applicationContext: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .cache(Cache(File(applicationContext.cacheDir, "httpCache"), 10 * 1024 * 1024)) // 10 MiB
                .addNetworkInterceptor(ProxerCacheRewriteInterceptor())
                .addNetworkInterceptor(CloudFlareInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
            builder.addNetworkInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    fun provideSeriesDbHelper(context: Context): SeriesDbHelper {
        return SeriesDbHelper(context)
    }

    @Provides
    @Singleton
    fun provideMySeriesRepository(dbHelper: SeriesDbHelper): MySeriesRepository {
        return MySeriesRepository(dbHelper)
    }

    @Provides
    fun provideServerConfig(): ServerConfig {
        return ServerConfig()
    }

    // automatically providing the ArrayList does not work properly
    fun provideStreamResolver(httpClient: OkHttpClient, gson: Gson): ArrayList<StreamResolver> {
        return arrayListOf(
                ProxerStreamResolver(httpClient),
                Mp4UploadStreamResolver(httpClient),
                StreamCloudResolver(httpClient),
                DailyMotionStreamResolver(httpClient, gson))
    }

    @Provides
    @Singleton
    fun provideProxerClient(httpClient: OkHttpClient,
                            gson: Gson,
                            serverConfig: ServerConfig): ProxerClient {

        return ProxerClient(httpClient, gson, provideStreamResolver(httpClient, gson), serverConfig)
    }
}

@Singleton
@Component(modules = arrayOf(BaseModule::class))
interface BaseComponent {
    fun getProxerClient(): ProxerClient
    fun getMySeriesRepository(): MySeriesRepository
}