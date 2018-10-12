package com.inverse.unofficial.proxertv.base.client

import android.app.Application
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.inverse.unofficial.proxertv.BuildConfig
import com.inverse.unofficial.proxertv.base.client.interceptors.ApiKeyInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.CloudFlareInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.NoCacheCaptchaInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.ProxerCacheRewriteInterceptor
import com.inverse.unofficial.proxertv.base.client.util.*
import com.inverse.unofficial.proxertv.model.CommentRatings
import com.inverse.unofficial.proxertv.model.ServerConfig
import com.inverse.unofficial.proxertv.model.typeAdapter.CommentRatingsTypeAdapter
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
object ClientModule {

    private const val CLIENT_DEFAULT = "default"
    private const val CLIENT_API = "api"
    private const val CLIENT_WEB = "web"
    const val CLIENT_GLIDE = "glide"

    @Provides
    @Singleton
    @JvmStatic
    fun provideServerConfig(): ServerConfig {
        return ServerConfig()
    }

    @Provides
    @Singleton
    @JvmStatic
    fun provideGson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(CommentRatings::class.java, CommentRatingsTypeAdapter().nullSafe())
                .create()
    }

    @Provides
    @Named(CLIENT_GLIDE)
    @JvmStatic
    fun provideGlideHttpClient(application: Application): OkHttpClient {
        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(application))

        return OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(CloudFlareInterceptor())
                .build()
    }

    @Provides
    @Singleton
    @Named(CLIENT_DEFAULT)
    @JvmStatic
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
    @Named(CLIENT_API)
    @JvmStatic
    fun provideApiHttpClient(@Named(CLIENT_DEFAULT) defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
                .addInterceptor(ApiKeyInterceptor(BuildConfig.PROXER_API_KEY)).build()
    }

    @Provides
    @Named(CLIENT_WEB)
    @JvmStatic
    fun provideWebHttpClient(@Named(CLIENT_DEFAULT) defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
                .addInterceptor(NoCacheCaptchaInterceptor()).build()

    }

    @Provides
    @JvmStatic
    fun provideProxerApi(@Named(CLIENT_API) httpClient: OkHttpClient,
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
    @JvmStatic
    fun provideProxerClient(@Named(CLIENT_WEB) httpClient: OkHttpClient,
                            api: ProxerApi,
                            gson: Gson,
                            serverConfig: ServerConfig): ProxerClient {

        return ProxerClient(httpClient, api, gson, provideStreamResolver(httpClient, gson), serverConfig)
    }
}