package com.inverse.unofficial.proxertv.ui.util

/**
 * Util for the common load content error state behavior.
 */
@Suppress("unused")
sealed class LoadContentErrorState<T> {
    abstract val data: T?
}

/**
 * Represents a loading state
 */
class LoadingState<T>(private val cachedData: T? = null) : LoadContentErrorState<T>() {
    override val data: T?
        get() = cachedData
}

/**
 * Represents an error state
 */
data class ErrorState<T>(private val lastData: T? = null, val error: Throwable? = null) : LoadContentErrorState<T>() {
    override val data: T?
        get() = lastData
}

/**
 * Represents a successful loading of [value]
 */
data class SuccessState<T>(private val value: T) : LoadContentErrorState<T>() {
    override val data: T
        get() = value
}