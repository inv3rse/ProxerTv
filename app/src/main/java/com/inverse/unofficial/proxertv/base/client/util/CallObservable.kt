package com.inverse.unofficial.proxertv.base.client.util

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import rx.Observable
import java.io.IOException

class CallObservable(val call: Call) : Observable<Response>(fun(subscriber) {
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
}) {
}