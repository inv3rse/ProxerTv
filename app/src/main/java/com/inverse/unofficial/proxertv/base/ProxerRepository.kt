package com.inverse.unofficial.proxertv.base

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesCover
import rx.Observable

/**
 * Repository combining the {@link ProxerClient} and local database
 */
class ProxerRepository(
        private val client: ProxerClient,
        private val mySeriesDb: MySeriesDb,
        private val progressDatabase: SeriesProgressDb,
        private val userSettings: UserSettings) {

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
                .retryWhen { errors: Observable<out Throwable> ->
                    errors.flatMap { error ->
                        if (error is ApiErrorException && ApiErrorException.OTHER_USER_ALREADY_SIGNED_IN == error.code)
                        // try again once after logout emits an item
                            client.logout() else
                        // abort with the original error
                            Observable.error(error as Throwable)
                    }
                }
                // save the login data (the backing SharedPreferences are thread safe)
                .doOnNext { login ->
                    userSettings.setUser(login.username, login.password)
                    userSettings.setUserToken(login.token)
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
     * Sync the series list if a user is signed in.
     *
     * @return an Observable emitting the current synchronized series list and any subsequent changes
     */
    fun syncUserList(): Observable<List<SeriesCover>> {
        val user = userSettings.getUser()
        if (user != null) {
            return client.getUserList()
                    // retry if if we are not logged in anymore
                    .retryWhen { errors: Observable<out Throwable> ->
                        errors.flatMap { error ->
                            if (error is ApiErrorException && ApiErrorException.USER_DOES_NOT_EXISTS == error.code)
                            // try again once after login emits an item
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
                    .flatMap { mySeriesDb.setSeries(it) }
                    // return db observable
                    .flatMap { mySeriesDb.observeSeriesList() }
        }

        // local db only if not logged in
        return mySeriesDb.observeSeriesList()
    }

    data class Login(
            val username: String,
            val password: String,
            val token: String?
    )
}

