package com.inverse.unofficial.proxertv.base.client.util

import com.inverse.unofficial.proxertv.model.WrappedData
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

/**
 * Converter to simplify working with the api.
 * The api contains nested data with a custom response code.
 * We only return the json data field containing the actual response object or throw an exception
 */
class ApiResponseConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        if (annotations.any { it is FailOnError }) {

            // delegate actual parsing to next converter (gson)
            val delegate = retrofit.nextResponseBodyConverter<WrappedData<*>>(this, WrappedData::class.java, annotations)
            return StatusApiResponseConverter(delegate)

        } else if (annotations.any { it is WrappedResponse }) {
            val wrappedType = Types.newParameterizedType(WrappedData::class.java, type)

            // delegate actual parsing to next converter (gson)
            val delegate = retrofit.nextResponseBodyConverter<WrappedData<*>>(this, wrappedType, annotations)
            return WrappedApiResponseConverter(delegate)
        }

        // we are not handling the response
        return null
    }

    /**
     * Converts a wrapped api response or throws an error.
     */
    class WrappedApiResponseConverter<T>(val delegate: Converter<ResponseBody, WrappedData<T>>) : Converter<ResponseBody, T> {

        override fun convert(value: ResponseBody): T {

            val wrappedData = delegate.convert(value) ?: throw IOException("Failed to get api response")
            if (wrappedData.error != 0 || wrappedData.data == null) {
                throw ApiErrorException(wrappedData.code, wrappedData.message)
            }

            return wrappedData.data
        }
    }

    /**
     * Returns true if the response is successful or throws an error.
     */
    class StatusApiResponseConverter(val delegate: Converter<ResponseBody, WrappedData<*>>) : Converter<ResponseBody, Boolean> {

        override fun convert(value: ResponseBody): Boolean {
            val wrappedData = delegate.convert(value) ?: throw IOException("Failed to get api response")
            if (wrappedData.error != 0) {
                throw ApiErrorException(wrappedData.code, wrappedData.message)
            }

            return true
        }
    }
}

/**
 * Annotation that wraps the model type into a [WrappedData] object and returns only the data object.
 * If the [WrappedData] does not contain a data object or the error is set an [ApiErrorException] will be thrown.
 */
annotation class WrappedResponse

/**
 * Annotation that expects an [WrappedData] Type and fails if the error is set with an [ApiErrorException].
 * Otherwise it returns true.
 */
annotation class FailOnError

/**
 * Error response from the proxer api.
 */
class ApiErrorException(val code: Int?, val msg: String?) : IOException("api error: $msg, code:$code") {

    companion object {
        const val MISSING_LOGIN_DATA = 3000
        const val INVALID_LOGIN_DATA = 3001
        const val USER_ALREADY_SIGNED_IN = 3012
        const val OTHER_USER_ALREADY_SIGNED_IN = 3013
        const val ENTRY_ALREADY_EXISTS = 3010

        // error codes that say not logged in
        val USER_NOT_LOGGED_IN = listOf(3003, 3004, 3009)
    }
}