package com.inverse.unofficial.proxertv.base.client

import com.inverse.unofficial.proxertv.base.client.util.WrappedResponse
import com.inverse.unofficial.proxertv.model.Episodes
import com.inverse.unofficial.proxertv.model.Series
import retrofit2.http.POST
import retrofit2.http.Query
import rx.Observable

/**
 * The Proxer api interface.
 * Because every request needs an api key as post parameter, all are of the POST type.
 * Theoretically GET does allow a body, but that is not really common and OkHttp does not support it.
 * We could define the endpoint as GET however because {@link ApiKeyInterceptor} does transform
 * it to a POST type when adding the api key.
 */
interface ProxerApi {

    @POST("info/entry")
    @WrappedResponse
    fun entryInfo(@Query("id") id: Int): Observable<Series>

    @POST("info/listinfo")
    @WrappedResponse
    fun entryEpisodes(@Query("id") id: Int, @Query("p") page: Int, @Query("limit") limit: Int): Observable<Episodes>
}