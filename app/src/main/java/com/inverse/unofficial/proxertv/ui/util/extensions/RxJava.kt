package com.inverse.unofficial.proxertv.ui.util.extensions

import hu.akarnokd.rxjava.interop.RxJavaInterop

/**
 * Convert the RxJava1 Observable to a RxJava2 Observable
 */
fun <T> rx.Observable<T>.toV2() = RxJavaInterop.toV2Observable(this)