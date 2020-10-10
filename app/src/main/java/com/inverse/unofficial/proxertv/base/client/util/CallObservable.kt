package com.inverse.unofficial.proxertv.base.client.util

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.ResponseBody
import rx.Observable
import java.io.IOException

class CallObservable(private val call: Call) : Observable<Response>(fun(subscriber) {
    call.enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (!subscriber.isUnsubscribed) {
                subscriber.onNext(response)
                subscriber.onCompleted()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!subscriber.isUnsubscribed) {
                subscriber.onError(e)
            }
        }
    })
})

/**
 * Exception for an unsuccessful response.
 */
class OkHttpException(response: Response) :
        Exception("${response.request.url}\n HTTP ${response.code} ${response.message}") {

    val code = response.code
}

/**
 * Exception for a missing response body.
 */
class EmptyBodyException : Exception("Response body was empty")

/**
 * Observe the http body of the [call]
 */
class BodyCallObservable(private val call: Call) : Observable<ResponseBody>(fun(subscriber) {
    call.enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (!subscriber.isUnsubscribed) {

                if (!response.isSuccessful) {
                    subscriber.onError(OkHttpException(response))
                    return
                }

                val body = response.body
                if (body != null) {
                    subscriber.onNext(body)
                    subscriber.onCompleted()
                } else {
                    subscriber.onError(EmptyBodyException())
                }
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!subscriber.isUnsubscribed) {
                subscriber.onError(e)
            }
        }
    })
})
