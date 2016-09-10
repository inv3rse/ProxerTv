package com.inverse.unofficial.proxertv.model

/**
 * Class that represents a wrapped api response
 */
data class WrappedData<out T>(
        val error: Int,
        val message: String,
        val data: T?,
        val code: Int?
)