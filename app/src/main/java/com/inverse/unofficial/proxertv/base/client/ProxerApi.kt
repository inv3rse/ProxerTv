package com.inverse.unofficial.proxertv.base.client

import com.inverse.unofficial.proxertv.base.client.util.WrappedResponse
import com.inverse.unofficial.proxertv.model.Episodes
import com.inverse.unofficial.proxertv.model.Series
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

/**
 * The Proxer api interface. The WrappedResponse annotation will only return the nested data object or throw an error.
 */
interface ProxerApi {

    @GET("info/entry")
    @WrappedResponse
    fun entryInfo(@Query("id") id: Int): Observable<Series>

    @GET("info/listinfo")
    @WrappedResponse
    fun entryEpisodes(@Query("id") id: Int, @Query("p") page: Int, @Query("limit") limit: Int): Observable<Episodes>
}