package com.inverse.unofficial.proxertv.base.client

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor to simplify working with the api.
 * The api contains nested data with a custom response code.
 * We map the error code to a corresponding http response code and only return the json data field
 * containing the actual object
 */
class DataToResponseCodeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain?): Response {
        throw UnsupportedOperationException("not implemented")
    }
}