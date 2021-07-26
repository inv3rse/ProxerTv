package com.inverse.unofficial.proxertv.ui.util.extensions

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.Observable
import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Single
import rx.SingleSubscriber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Convert the RxJava1 Observable to a RxJava2 Observable
 */
fun <T> rx.Observable<T>.toV2(): Observable<T> = RxJavaInterop.toV2Observable(this)

/**
 * Await the completion of this [Single] without blocking the thread.
 * Returns the success value or throws the received error.
 */
suspend fun <T> Single<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        val subscription = subscribe(object : SingleSubscriber<T>() {
            override fun onSuccess(value: T) {
                continuation.resume(value)
            }

            override fun onError(error: Throwable) {
                continuation.resumeWithException(error)
            }
        })

        continuation.invokeOnCancellation { subscription.unsubscribe() }
    }
}