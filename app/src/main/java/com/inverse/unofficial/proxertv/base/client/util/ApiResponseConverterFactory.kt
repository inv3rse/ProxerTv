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
        if (annotations.none({ annotation -> annotation is WrappedResponse })) {
            // we do not handle this type
            return null
        }

        val wrappedType = Types.newParameterizedType(WrappedData::class.java, type)

        // delegate actual parsing to next converter (gson)
        val delegate = retrofit.nextResponseBodyConverter<WrappedData<*>>(this, wrappedType, annotations)
        return ApiResponseConverter(delegate)
    }

    class ApiResponseConverter<T>(val delegate: Converter<ResponseBody, WrappedData<T>>) : Converter<ResponseBody, T> {

        override fun convert(value: ResponseBody): T {

            val wrappedData = delegate.convert(value) ?: throw IOException("Failed to get api response")
            if (wrappedData.data == null) {
                throw ApiErrorException(wrappedData.message)
            }

            return wrappedData.data
        }
    }
}

annotation class WrappedResponse

class ApiErrorException(msg: String?) : IOException("api error: $msg")