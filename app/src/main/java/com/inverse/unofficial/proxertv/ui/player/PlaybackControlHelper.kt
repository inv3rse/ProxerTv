package com.inverse.unofficial.proxertv.ui.player

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.support.v17.leanback.app.PlaybackControlGlue
import android.support.v17.leanback.widget.PlaybackControlsRow
import android.view.KeyEvent
import android.view.View

class PlaybackControlHelper(
        val overlayFragment: PlayerOverlayFragment,
        context: Context) : PlaybackControlGlue(context, overlayFragment, SEEK_SPEEDS) {

    private lateinit var transportControls: MediaController.TransportControls
    private var metaData: MediaMetadata? = null
    private var playbackState: PlaybackState? = null
    private var currentSpeed = PLAYBACK_SPEED_NORMAL

    private var seekActive = false
    private var seekProgress = -1L
    private val handler = Handler(Looper.getMainLooper())
    private val seekRunner = object : Runnable {
        override fun run() {
            if (seekProgress == -1L) {
                seekProgress = currentPosition.toLong()
            }
            seekProgress += +(1000 * currentSpeed)
            updateProgress()

            handler.postDelayed(this, 100)
        }
    }

    init {
        transportControls = fragment.activity.mediaController.transportControls
    }

    override fun isMediaPlaying(): Boolean {
        return isStatePlaying(playbackState?.state ?: PlaybackState.STATE_NONE)
    }

    override fun pausePlayback() {
        transportControls.pause()
    }

    override fun startPlayback(speed: Int) {
        currentSpeed = speed
        if ((speed < PLAYBACK_SPEED_PAUSED) or (speed > PLAYBACK_SPEED_NORMAL)) {
            startSeek()
        } else {
            if (seekActive) {
                stopSeek()
            }
            if (currentSpeed == PLAYBACK_SPEED_NORMAL) {
                transportControls.play()
            }
        }
    }

    override fun skipToPrevious() {
        transportControls.skipToPrevious()
    }

    override fun skipToNext() {
        transportControls.skipToNext()
    }

    override fun getMediaArt() = null

    override fun hasValidMedia(): Boolean {
        return metaData != null
    }

    override fun getMediaTitle(): CharSequence {
        return metaData!!.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
    }

    override fun getMediaSubtitle(): CharSequence {
        return metaData!!.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
    }

    override fun onRowChanged(row: PlaybackControlsRow?) {
        // do nothing
    }

    override fun getCurrentSpeedId(): Int {
        return if (isMediaPlaying) currentSpeed else PLAYBACK_SPEED_PAUSED
    }

    override fun getCurrentPosition(): Int {
        return playbackState?.position?.toInt() ?: 0
    }

    private fun getBufferedPosition(): Int {
        return playbackState?.bufferedPosition?.toInt() ?: 0
    }

    override fun getMediaDuration(): Int {
        return metaData?.let { it.getLong(MediaMetadata.METADATA_KEY_DURATION).toInt() } ?: 0
    }

    override fun getSupportedActions(): Long {
        return (PlaybackControlGlue.ACTION_PLAY_PAUSE or
                PlaybackControlGlue.ACTION_FAST_FORWARD or
                PlaybackControlGlue.ACTION_REWIND).toLong()
    }

    override fun updateProgress() {
        if (seekActive) {
            controlsRow.currentTime = seekProgress.toInt()
        } else {
            controlsRow.currentTime = currentPosition
        }
        controlsRow.bufferedProgress = getBufferedPosition()
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        if (seekActive and ((event.action == KeyEvent.ACTION_UP) or event.isCanceled)) {
            stopSeek()
            return true
        }
        return super.onKey(v, keyCode, event)
    }

    fun createMediaControllerCallback(): MediaController.Callback {
        return MediaControllerCallback()
    }

    private fun isStatePlaying(state: Int): Boolean {
        return (state == PlaybackState.STATE_BUFFERING) or
                (state == PlaybackState.STATE_CONNECTING) or
                (state == PlaybackState.STATE_PLAYING) or
                (state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS) or
                (state == PlaybackState.STATE_SKIPPING_TO_NEXT)
    }

    private fun startSeek() {
        if (!seekActive) {
            seekActive = true
            seekProgress = -1L

            handler.post(seekRunner)
        }
    }

    private fun stopSeek() {
        handler.removeCallbacks(seekRunner)
        seekActive = false
        transportControls.seekTo(seekProgress)

        currentSpeed = if (isMediaPlaying) PLAYBACK_SPEED_NORMAL else PLAYBACK_SPEED_PAUSED
        onStateChanged()
    }

    private inner class MediaControllerCallback : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState) {
            // Update your UI to reflect the new state. Do not change media playback here.
            playbackState = state
            val nextState = state.state
            if (nextState != PlaybackState.STATE_NONE) {
                updateProgress()
            }
            onStateChanged()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            this@PlaybackControlHelper.metaData = metadata
            this@PlaybackControlHelper.onMetadataChanged() // Update metadata on controls.
            overlayFragment.updatePlaybackRow()
        }
    }

    companion object {
        private val SEEK_SPEEDS = intArrayOf(2)
    }
}