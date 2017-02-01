package com.inverse.unofficial.proxertv.base

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import com.inverse.unofficial.proxertv.model.*
import rx.Observable
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository combining the [ProxerClient] and local databases ([MySeriesDb], [SeriesProgressDb]).
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
     * @return the [Observable] emitting the login object or throwing an error
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
                // logout if an other user was signed in and try again
                .compose(retryAfter<Login>(MAX_LOGIN_RETRY_COUNT, { errorCount: Int, error: Throwable ->
                    if (errorCount < MAX_LOGIN_RETRY_COUNT && error is ApiErrorException
                            && ApiErrorException.OTHER_USER_ALREADY_SIGNED_IN == error.code) {

                        // retry after logout
                        client.logout()
                    } else {
                        // abort with the original error
                        Observable.error<Login>(error)
                    }
                }))
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
     * @return an [Observable] emitting true or throwing an error
     */
    fun logout(): Observable<Boolean> {
        userSettings.clearUser()
        return client.logout().map { true }
    }

    /**
     * Sync the series list if a user is signed in and 30 minutes have passed since the last sync.
     *
     * @param forceSync sync even if the time has not passed jet
     * @return an [Observable] emitting true if the list was synced
     */
    fun syncUserList(forceSync: Boolean = false): Observable<Boolean> {
        val user = userSettings.getUser()

        // only sync if we are logged in and LIST_SYNC_DELAY has passed or forced
        if (user != null && (forceSync || System.currentTimeMillis() > (lastSync.get() + LIST_SYNC_DELAY))) {

            return client.userList()
                    // retry after login if if we are not logged in anymore
                    .compose(retryAfterLogin<List<UserListSeriesEntry>>())
                    // write entries to db
                    .flatMap { setUserSeriesList(it) }
                    // not successful on error
                    .onErrorReturn { error ->
                        CrashReporting.logException(error)
                        false
                    }
        }

        // no sync has happened
        return Observable.just(false)
    }

    private fun setUserSeriesList(seriesEntries: List<UserListSeriesEntry>): Observable<Boolean> {
        return Observable.from(seriesEntries)
                // filter only active and bookmarked
                .filter { UserListSeriesEntry.MEDIUM_ANIME == it.medium }
                // map to db entries
                .map { SeriesDbEntry(it.id, it.name, SeriesList.fromApiState(it.commentState), it.cid) }
                .toList()
                // write to db
                .flatMap { seriesList -> mySeriesDb.overrideWithSeriesList(seriesList) }
                // return true for successful sync and save sync time
                .map {
                    lastSync.set(System.currentTimeMillis())
                    true
                }
    }

    /**
     * Moves the series to the specified list. If there is no entry for the series one will be created,
     * otherwise the existing will be updated. A series can only be on one or zero lists.
     * @param series the series to update
     * @param list the list to add the series to
     */
    fun moveSeriesToList(series: ISeriesCover, list: SeriesList): Observable<Unit> {
        if (list == SeriesList.NONE) {
            return removeSeriesFromList(series.id)
        }

        if (userSettings.getUser() != null) {
            // check if the series is already on a list and has a comment id
            val updateObservable = mySeriesDb.getSeries(series.id)
                    // the series must have a comment id
                    .filter { it.cid != SeriesDbEntry.NO_COMMENT_ID }
                    // throw an error if there is no item
                    .first()
                    // we do not care about the type, only check if there was an error
                    .cast(Any::class.java)
                    // no comment id found -> create a comment for the series
                    .onErrorResumeNext { error1 ->
                        client.addSeriesToWatchList(series.id)
                                // ignore the comment already exists error.
                                .onErrorResumeNext { error2 ->
                                    if (error2 is ApiErrorException && error2.code != ApiErrorException.ENTRY_ALREADY_EXISTS) {
                                        Observable.just(true)
                                    } else {
                                        Observable.error(error2)
                                    }
                                }
                    }
                    // get the users current series list
                    .flatMap { client.userList() }
                    // find the item we want to change
                    .map { seriesList ->
                        val entry = seriesList.find { series.id == it.id } ?: throw NoSuchElementException("comment id not found")
                        Pair(entry, seriesList)
                    }
                    // update the list state if necessary
                    .flatMap { pair: Pair<UserListSeriesEntry, List<UserListSeriesEntry>> ->
                        if (SeriesList.fromApiState(pair.first.commentState) != list) {
                            val entry = pair.first.copy(commentState = SeriesList.toApiState(list))
                            val data = entry.commentRating
                            val comment = Comment(entry.commentState, entry.episode, entry.rating,
                                    entry.comment, data?.ratingGenre, data?.ratingStory, data?.ratingAnimation,
                                    data?.ratingCharacters, data?.ratingMusic)

                            // update the entry list to reflect the state updating the comment
                            val updatedEntryList = mutableListOf(entry)
                            updatedEntryList.addAll(pair.second.filter { it.id != entry.id })

                            client.setComment(entry.cid, comment)
                                    .flatMap { setUserSeriesList(updatedEntryList) }
                                    .map { Unit }
                        } else {
                            // no change necessary, use the data to synchronize the local db
                            setUserSeriesList(pair.second).map { Unit }
                        }
                    }

            return updateObservable
                    .compose(retryAfterLogin<Unit>())
                    .doOnError { CrashReporting.logException(it) }
                    .onErrorResumeNext { syncUserList(true).map { Unit } }

        } else {
            // local only, add the series without a comment id
            val dbEntry = SeriesDbEntry(series.id, series.name, list, SeriesDbEntry.NO_COMMENT_ID)
            return mySeriesDb.insertOrUpdateSeries(dbEntry)
        }
    }

    /**
     * Remove a series from the users list
     * @param seriesId the id of the series
     * @return an [Observable] emitting onError or OnCompleted
     */
    fun removeSeriesFromList(seriesId: Int): Observable<Unit> {
        if (userSettings.getUser() != null) {
            // get the users comment id for the series
            mySeriesDb.getSeries(seriesId)
                    .filter { it.cid != SeriesDbEntry.NO_COMMENT_ID }


        }

        return mySeriesDb.removeSeries(seriesId)
    }

    /**
     * Get the progress for a series.
     * @param seriesId the id of the series
     * @return an [Observable] emitting the progress
     */
    fun getSeriesProgress(seriesId: Int): Observable<Int> {
        return progressDatabase.getProgress(seriesId)
    }

    /**
     * Set the progress for a series.
     * @param seriesId the id of the series
     * @param progress the progress to set
     * @return an [Observable] emitting onError or OnCompleted
     */
    fun setSeriesProgress(seriesId: Int, progress: Int): Observable<Unit> {
        if (userSettings.getUser() != null) {
            client.invalidateSeriesCache(seriesId)
        }

        return progressDatabase.setProgress(seriesId, progress)
    }

    /**
     * Load the top access series list.
     * @return an [Observable] emitting the series list
     */
    fun loadTopAccessSeries() = client.loadTopAccessSeries()

    /**
     * Load the top rating series list.
     * @return an [Observable] emitting the series list
     */
    fun loadTopRatingSeries() = client.loadTopRatingSeries()

    /**
     * Load the top rating movies list.
     * @return an [Observable] emitting the movies list
     */
    fun loadTopRatingMovies() = client.loadTopRatingMovies()

    /**
     * Load the top airing series list.
     * @return an [Observable] emitting the series list
     */
    fun loadAiringSeries() = client.loadAiringSeries()

    /**
     * Load the list of series updates.
     * @return an [Observable] emitting the updates list
     */
    fun loadUpdatesList() = client.loadUpdatesList()

    /**
     * Loads the available episodes map by sub/dub type
     * @param seriesId the id of the series
     * @param page the page to load (first page is 0)
     * @return an [Observable] emitting the available episodes by sub/dub type
     */
    fun loadEpisodesPage(seriesId: Int, page: Int) = client.loadEpisodesPage(seriesId, page)

    /**
     * Load the series detail information
     * @param id the series id
     * @return an Observable emitting the Series
     */
    fun loadSeries(id: Int) = client.loadSeries(id)

    /**
     * Observe the series list.
     * @return an [Observable] emitting the current value and any subsequent changes
     */
    fun observeSeriesList() = mySeriesDb.observeSeriesList()

    /**
     * Check if the list contains a specific series
     * @param seriesId id of the series to check for
     * @return an [Observable] emitting true or false
     */
    fun hasSeriesOnList(seriesId: Int) = mySeriesDb.containsSeries(seriesId)

    /**
     * Observe the progress for a series
     * @param seriesId series to get the progress for
     * @return an [Observable] emitting the progress and any subsequent changes
     */
    fun observeSeriesProgress(seriesId: Int) = progressDatabase.observeProgress(seriesId)

    /**
     * Force a sync the next time syncUserList is called.
     */
    private fun invalidateLocalList() {
        lastSync.set(0)
    }

    data class Login(
            val username: String,
            val password: String,
            val token: String?
    )

    /**
     * Get a [Observable.Transformer] that adds the re login logic if the call fails with any error in
     * [ApiErrorException.USER_NOT_LOGGED_IN]
     */
    private fun <T> retryAfterLogin(): Observable.Transformer<T, T> {
        return retryAfter(MAX_LOGIN_RETRY_COUNT, { errorCount: Int, error: Throwable ->
            val user = userSettings.getUser()
            if (errorCount < MAX_LOGIN_RETRY_COUNT
                    && error is ApiErrorException
                    && ApiErrorException.USER_NOT_LOGGED_IN.contains(error.code)
                    && user != null) {

                // retry again after login
                login(user.username, user.password)
            } else {
                // abort with the original error
                Observable.error(error)
            }
        })
    }

    /**
     * Get a [Observable.Transformer] that adds the retry logic
     * @param maxCount the max retry count
     * @param checkFun the function that decides if we try again.
     *      If the Observable emits onNext we retry
     *      onError or onCompleted get passed to the subscriber directly
     */
    private fun <T> retryAfter(maxCount: Int, checkFun: (count: Int, error: Throwable) -> Observable<*>): Observable.Transformer<T, T> {
        return Observable.Transformer<T, T> { observable ->
            observable.retryWhen { notificationObservable: Observable<out Throwable> ->
                notificationObservable
                        // zip with retry counter
                        .zipWith(Observable.range(0, maxCount + 1), { error, count -> Pair(error, count) })
                        // let the checkFun decide if we retry
                        .flatMap { pair ->
                            val error = pair.first
                            val count = pair.second

                            checkFun(count, error)
                        }
            }
        }
    }

    companion object {
        private const val LIST_SYNC_DELAY = 30 * 60 * 60 * 1000 // 30 min in millis
        private const val MAX_LOGIN_RETRY_COUNT = 1
    }
}

