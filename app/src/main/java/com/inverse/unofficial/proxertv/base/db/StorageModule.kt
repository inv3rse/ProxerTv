package com.inverse.unofficial.proxertv.base.db

import android.app.Application
import com.inverse.unofficial.proxertv.Database
import com.inverse.unofficial.proxertv.sql.SystemSeriesListEntry
import com.inverse.unofficial.proxertv.sql.UserSeriesListEntry
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object StorageModule {

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

    @Provides
    fun provideSeriesDb(application: Application): Database {
        val driver = AndroidSqliteDriver(Database.Schema, application, "series.db")
        return Database(
            driver = driver,
            systemSeriesListEntryAdapter = SystemSeriesListEntry.Adapter(listTypeAdapter = EnumColumnAdapter()),
            userSeriesListEntryAdapter = UserSeriesListEntry.Adapter(listTypeAdapter = EnumColumnAdapter())
        )
    }
}