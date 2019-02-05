package com.inverse.unofficial.proxertv.ui.player

import android.media.session.MediaController
import android.os.Bundle
import com.inverse.unofficial.proxertv.model.Stream

object CustomPlayerAction {
    const val PLAY_STREAM = "PLAY_STREAM"
    const val KEY_STREAM_URL = "KEY_STREAM_URL"
    const val KEY_PROVIDER_NAME = "KEY_PROVIDER_NAME"
}

fun MediaController.TransportControls.playStream(stream: Stream) {
    val extras = Bundle()
    extras.putString(CustomPlayerAction.KEY_STREAM_URL, stream.streamUrl)
    extras.putString(CustomPlayerAction.KEY_PROVIDER_NAME, stream.providerName)

    sendCustomAction(CustomPlayerAction.PLAY_STREAM, extras)
}