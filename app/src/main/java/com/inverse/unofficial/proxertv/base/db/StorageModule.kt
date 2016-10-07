package com.inverse.unofficial.proxertv.base.db

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class StorageModule {

    @Provides
    fun provideSeriesDbHelper(application: Application): SeriesDbHelper {
        return SeriesDbHelper(application)
    }

    @Provides
    fun provideSeriesProgressDbHelper(application: Application): SeriesProgressDbHelper {
        return SeriesProgressDbHelper(application)
    }

    @Provides
    @Singleton
    fun provideMySeriesRepository(dbHelper: SeriesDbHelper): MySeriesDb {
        return MySeriesDb(dbHelper)
    }

    @Provides
    @Singleton
    fun provideSeriesProgressRepository(dbHelper: SeriesProgressDbHelper): SeriesProgressDb {
        return SeriesProgressDb(dbHelper)
    }
}