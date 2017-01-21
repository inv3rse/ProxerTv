package com.inverse.unofficial.proxertv.base.client

import com.inverse.unofficial.proxertv.base.client.util.FailOnError
import com.inverse.unofficial.proxertv.base.client.util.WrappedResponse
import com.inverse.unofficial.proxertv.model.Episodes
import com.inverse.unofficial.proxertv.model.LoginResponse
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.WrappedData
import retrofit2.http.*
import rx.Observable

/**
 * The Proxer api interface. The WrappedResponse annotation will only return the nested data object or throw an error.
 */
interface ProxerApi {

    /**
     * Login with username and password.
     * If the login is successful a cookie will be set.
     *
     * @param username the username
     * @param password the password
     */
    @POST("user/login")
    @WrappedResponse
    @FormUrlEncoded
    fun login(@Field("username") username: String, @Field("password") password: String): Observable<LoginResponse>

    /**
     * Logout the user specified via cookie.
     */
    @POST("user/logout")
    fun logout(): Observable<WrappedData<Unit>>

    /**
     * Get the list of series for the currently logged in user.
     * @note Caching is disabled
     */
    @GET("user/list")
    @Headers("Cache-Control: no-cache")
    @WrappedResponse
    fun userList(): Observable<List<Series>>

    /**
     * Get the info for a series.
     *
     * @param id the id of the series
     */
    @GET("info/entry")
    @WrappedResponse
    fun entryInfo(@Query("id") id: Int): Observable<Series>

    /**
     * Get the info for a series.
     *
     * @param id the id of the series
     * @param cacheControl the cache control header
     */
    @GET("info/entry")
    @WrappedResponse
    fun entryInfo(@Query("id") id: Int, @Header("Cache-Control") cacheControl: String): Observable<Series>

    /**
     * Get the list of episodes for a series.
     *
     * @param id the id of the series
     * @param page the page index
     * @param limit the number of entries per page
     */
    @GET("info/listinfo")
    @WrappedResponse
    fun entryEpisodes(@Query("id") id: Int, @Query("p") page: Int, @Query("limit") limit: Int): Observable<Episodes>

    /**
     * Set the progress of the comment for a series
     * @param commentId the comment id of the user for a series. Not the series id.
     * @param progress the progress of the series
     */
    @POST("ucp/setcommentstate")
    @FailOnError
    @FormUrlEncoded
    fun setCommentState(@Field("id") commentId: Int, @Field("value") progress: Int): Observable<Boolean>
}