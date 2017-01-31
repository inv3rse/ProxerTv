package com.inverse.unofficial.proxertv.base.client

import android.app.Application
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.github.salomonbrys.kotson.registerNullableTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.inverse.unofficial.proxertv.BuildConfig
import com.inverse.unofficial.proxertv.base.client.interceptors.ApiKeyInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.CloudFlareInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.NoCacheCaptchaInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.ProxerCacheRewriteInterceptor
import com.inverse.unofficial.proxertv.base.client.util.*
import com.inverse.unofficial.proxertv.model.ServerConfig
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
class ClientModule {

    @Provides
    @Singleton
    fun provideServerConfig(): ServerConfig {
        return ServerConfig()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    @Named("default")
    fun provideDefaultHttpClient(application: Application,
                                 serverConfig: ServerConfig): OkHttpClient {

        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(application))

        val builder = OkHttpClient.Builder()
                .cache(Cache(File(application.cacheDir, "httpCache"), 10 * 1024 * 1024)) // 10 MiB
                .cookieJar(cookieJar)
                .addNetworkInterceptor(ProxerCacheRewriteInterceptor(serverConfig))
                .addInterceptor(CloudFlareInterceptor())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addNetworkInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Named("api")
    fun provideApiHttpClient(@Named("default") defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
                .addInterceptor(ApiKeyInterceptor(BuildConfig.PROXER_API_KEY)).build()
    }

    @Provides
    @Named("web")
    fun provideWebHttpClient(@Named("default") defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
                .addInterceptor(NoCacheCaptchaInterceptor()).build()

    }

    @Provides
    fun provideProxerApi(@Named("api") httpClient: OkHttpClient,
                         gson: Gson,
                         serverConfig: ServerConfig): ProxerApi {

        val retrofit = Retrofit.Builder()
                .baseUrl(serverConfig.apiBaseUrl)
                .client(httpClient)
                .addConverterFactory(ApiResponseConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()

        return retrofit.create(ProxerApi::class.java)
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
    fun provideProxerClient(@Named("web") httpClient: OkHttpClient,
                            api: ProxerApi,
                            gson: Gson,
                            serverConfig: ServerConfig): ProxerClient {

        return ProxerClient(httpClient, api, gson, provideStreamResolver(httpClient, gson), serverConfig)
    }
}