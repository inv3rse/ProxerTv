package com.inverse.unofficial.proxertv.model

data class Stream(
        val streamUrl: String,
        val providerName: String) {

    override fun equals(other: Any?): Boolean {
        return other is Stream && streamUrl == other.streamUrl
    }

    override fun hashCode(): Int {
        return streamUrl.hashCode()
    }
}