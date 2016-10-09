package com.inverse.unofficial.proxertv.base

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesCover
import rx.Observable
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository combining the {@link ProxerClient} and local databases.
 * Handles the login and synchronizes the local db with online data.
 */
class ProxerRepository(
        private val client: ProxerClient,
        private val mySeriesDb: MySeriesDb,
        private val progressDatabase: SeriesProgressDb,
        private val userSettings: UserSettings) {

    private var lastSync: AtomicLong = AtomicLong(0L)

    /**
     * Login with username and password.
     *
     * @param username the username
     * @param password the password
     * @return the Observable emitting the login object or throwing an error
     */
    fun login(username: String, password: String): Observable<Login> {

        return client.login(username, password)
                .map { Login(username, password, it.token) }
                // handle the already signed in error as a successful login
                .onErrorResumeNext { error ->
                    if (error is ApiErrorException && ApiErrorException.USER_ALREADY_SIGNED_IN == error.code)
                        Observable.just(Login(username, password, userSettings.getUserToken())) else
                        Observable.error(error)
                }
                // retry if another user was signed in after we log him out
                .retryWhen {
                    // pair error with retry counter
                    it.zipWith(Observable.range(0, 10), { error, count -> Pair(error, count) })
                            .flatMap { pair ->
                                val error = pair.first
                                val count = pair.second
                                // retry at most once
                                if (count < 1 && error is ApiErrorException && ApiErrorException.OTHER_USER_ALREADY_SIGNED_IN == error.code)
                                // logout
                                    client.logout() else
                                // abort with the original error
                                    Observable.error(error as Throwable)
                            }
                }
                // save the login data (the backing SharedPreferences are thread safe)
                .doOnNext { login ->
                    userSettings.setUser(login.username, login.password)
                    userSettings.setUserToken(login.token)
                    invalidateLocalList()
                }
    }

    /**
     * Log the current user out.
     *
     * @return an Observable emitting true or throwing an error
     */
    fun logout(): Observable<Boolean> {
        userSettings.clearUser()
        return client.logout().map { true }
    }

    /**
     * Sync the series list if a user is signed in and 30 minutes have passed since the last sync.
     *
     * @param forceSync sync even if the time has not passed jet
     * @return an Observable emitting true if the list was synced
     */
    fun syncUserList(forceSync: Boolean = false): Observable<Boolean> {
        val user = userSettings.getUser()

        // only sync if we are logged in and LIST_SYNC_DELAY has passed or forced
        if (user != null && (forceSync || System.currentTimeMillis() > (lastSync.get() + LIST_SYNC_DELAY))) {
            return client.getUserList()
                    // retry if if we are not logged in anymore
                    .retryWhen {
                        // retry counter
                        it.zipWith(Observable.range(0, 10), { error, count -> Pair(error, count) })
                                .flatMap { pair ->
                                    val error = pair.first
                                    val count = pair.second
                                    // retry at most once
                                    if (count < 1 && error is ApiErrorException && ApiErrorException.USER_DOES_NOT_EXISTS == error.code)
                                    // login again
                                        login(user.username, user.password) else
                                    // abort with the original error
                                        Observable.error(error as Throwable)
                                }
                    }
                    // list to single elements
                    .flatMap { Observable.from(it) }
                    // filter only active and bookmarked
                    .filter { Series.STATE_USER_BOOKMARKED == it.state || Series.STATE_USER_WATCHING == it.state }
                    .toList()
                    // write to db
                    .flatMap { seriesList -> mySeriesDb.setSeries(seriesList) }
                    // return true for successful sync and save sync time
                    .map {
                        lastSync.set(System.currentTimeMillis())
                        true
                    }
                    // not successful on error
                    .onErrorReturn { error ->
                        CrashReporting.logException(error)
                        false
                    }
        }

        // no sync has happened
        return Observable.just(false)
    }

    /**
     * Force a sync the next time syncUserList is called.
     */
    fun invalidateLocalList() {
        lastSync.set(0)
    }

    fun removeSeriesFromList(seriesId: Int): Observable<Unit> {
        return mySeriesDb.removeSeries(seriesId)
    }

    fun addSeriesToList(series: SeriesCover): Observable<Unit> {
        return mySeriesDb.addSeries(series)
    }

    fun getSeriesProgress(seriesId: Int): Observable<Int> {
        return progressDatabase.getProgress(seriesId)
    }

    fun setSeriesProgress(seriesId: Int, progress: Int): Observable<Unit> {
        return progressDatabase.setProgress(seriesId, progress)
    }

    fun loadTopAccessSeries() = client.loadTopAccessSeries()

    fun loadTopRatingSeries() = client.loadTopRatingSeries()

    fun loadTopRatingMovies() = client.loadTopRatingMovies()

    fun loadAiringSeries() = client.loadAiringSeries()

    fun loadUpdatesList() = client.loadUpdatesList()

    /**
     * Returns the available episodes by sub/dub type
     */
    fun loadEpisodesPage(seriesId: Int, page: Int) = client.loadEpisodesPage(seriesId, page)

    fun loadSeries(id: Int) = client.loadSeries(id)

    fun observeSeriesList() = mySeriesDb.observeSeriesList()

    fun hasSeriesOnList(seriesId: Int) = mySeriesDb.containsSeries(seriesId)

    fun observeSeriesProgress(seriesId: Int) = progressDatabase.observeProgress(seriesId)

    data class Login(
            val username: String,
            val password: String,
            val token: String?
    )

    companion object {
        const val LIST_SYNC_DELAY = 30 * 60 * 60 * 1000 // 30 min in millis
    }
}

