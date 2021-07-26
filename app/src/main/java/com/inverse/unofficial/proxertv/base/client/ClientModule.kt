package com.inverse.unofficial.proxertv.base.client

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.inverse.unofficial.proxertv.BuildConfig
import com.inverse.unofficial.proxertv.base.client.interceptors.ApiKeyInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.NoCacheCaptchaInterceptor
import com.inverse.unofficial.proxertv.base.client.interceptors.OldCloudFlareInterceptor
import com.inverse.unofficial.proxertv.base.client.util.AndroidWebViewCookieJar
import com.inverse.unofficial.proxertv.base.client.util.ApiResponseConverterFactory
import com.inverse.unofficial.proxertv.base.client.util.CloudFlareInterceptor
import com.inverse.unofficial.proxertv.base.client.util.DailyMotionStreamResolver
import com.inverse.unofficial.proxertv.base.client.util.Mp4UploadStreamResolver
import com.inverse.unofficial.proxertv.base.client.util.ProxerStreamResolver
import com.inverse.unofficial.proxertv.base.client.util.StreamCloudResolver
import com.inverse.unofficial.proxertv.base.client.util.StreamResolver
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
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
object ClientModule {

    @Qualifier
    annotation class DefaultHttpClient

    @Qualifier
    annotation class ApiHttpClient

    @Qualifier
    annotation class GlideHttpClient

    @Qualifier
    annotation class WebHttpClient

    @Provides
    @Singleton
    fun provideServerConfig(): ServerConfig {
        return ServerConfig()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(CommentRatings::class.java, CommentRatingsTypeAdapter().nullSafe())
            .create()
    }

    @Provides
    @GlideHttpClient
    fun provideGlideHttpClient(
        cookieJar: AndroidWebViewCookieJar,
        cloudFlareInterceptor: CloudFlareInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(cloudFlareInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @DefaultHttpClient
    fun provideDefaultHttpClient(
        application: Application,
        cookieJar: AndroidWebViewCookieJar,
        serverConfig: ServerConfig,
        cloudFlareInterceptor: CloudFlareInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cache(Cache(File(application.cacheDir, "httpCache"), 10 * 1024 * 1024)) // 10 MiB
            .cookieJar(cookieJar)
//            .addNetworkInterceptor(ProxerCacheRewriteInterceptor(serverConfig))
//            .addInterceptor(cloudFlareInterceptor)
            .addInterceptor(OldCloudFlareInterceptor())
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
    @ApiHttpClient
    fun provideApiHttpClient(@DefaultHttpClient defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.PROXER_API_KEY)).build()
    }

    @Provides
    @WebHttpClient
    fun provideWebHttpClient(@DefaultHttpClient defaultClient: OkHttpClient): OkHttpClient {
        return defaultClient.newBuilder()
            .addInterceptor(NoCacheCaptchaInterceptor()).build()
    }

    @Provides
    fun provideProxerApi(
        @ApiHttpClient httpClient: OkHttpClient,
        gson: Gson,
        serverConfig: ServerConfig
    ): ProxerApi {
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
            DailyMotionStreamResolver(httpClient, gson)
        )
    }

    @Provides
    @Singleton
    fun provideProxerClient(
        @WebHttpClient httpClient: OkHttpClient,
        api: ProxerApi,
        gson: Gson,
        serverConfig: ServerConfig
    ): ProxerClient {
        return ProxerClient(httpClient, api, gson, provideStreamResolver(httpClient, gson), serverConfig)
    }
}