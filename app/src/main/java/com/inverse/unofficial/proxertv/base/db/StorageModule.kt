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
    @Singleton
    fun provideMySeriesRepository(dbHelper: SeriesDbHelper): MySeriesRepository {
        return MySeriesRepository(dbHelper)
    }
}