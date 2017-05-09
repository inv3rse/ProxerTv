package com.inverse.unofficial.proxertv.model

import okhttp3.HttpUrl

data class Stream(
        val streamUrl: HttpUrl,
        val providerName: String) {

    constructor(streamUrl: String, providerName: String) : this(HttpUrl.parse(streamUrl), providerName)

    override fun equals(other: Any?): Boolean {
        return other is Stream && streamUrl == other.streamUrl
    }

    override fun hashCode(): Int {
        return streamUrl.hashCode()
    }
}