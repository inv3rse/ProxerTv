package com.inverse.unofficial.proxertv.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.support.v17.leanback.widget.*
import timber.log.Timber

class PlaybackControlsHelper(context: Context, val overlayFragment: PlayerOverlayFragment) {
    private val playPauseAction = PlaybackControlsRow.PlayPauseAction(context)
    private val rewindAction = PlaybackControlsRow.RewindAction(context)
    private val fastForwardAction = PlaybackControlsRow.FastForwardAction(context)
    private val pictureInPictureAction = PlaybackControlsRow.PictureInPictureAction(context)

    private lateinit var transportControls: MediaController.TransportControls

    private var metaData: MediaMetadata? = null
    private var playbackState: PlaybackState? = null

    lateinit var actionsAdapter: ArrayObjectAdapter
    lateinit var controlsRow: PlaybackControlsRow private set
    lateinit var controlsRowPresenter: PlaybackControlsRowPresenter private set

    init {
        transportControls = overlayFragment.activity.mediaController.transportControls
        buildRowAndPresenter(context)
    }

    fun createMediaControllerCallback(): MediaController.Callback {
        return MediaControllerCallback()
    }

    private fun buildRowAndPresenter(context: Context) {
        controlsRow = PlaybackControlsRow(this)
        actionsAdapter = ArrayObjectAdapter(ControlButtonPresenterSelector())
        controlsRow.primaryActionsAdapter = actionsAdapter

        actionsAdapter.add(rewindAction)
        actionsAdapter.add(playPauseAction)
        actionsAdapter.add(fastForwardAction)

        if (PlayerActivity.supportsPictureInPicture(context)) {
            actionsAdapter.add(pictureInPictureAction)
        }

        val detailsPresenter = object : AbstractDetailsDescriptionPresenter() {
            override fun onBindDescription(viewHolder: AbstractDetailsDescriptionPresenter.ViewHolder, item: Any) {
                viewHolder.title.text = metaData?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
                viewHolder.subtitle.text = metaData?.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
            }
        }

        controlsRowPresenter = object : PlaybackControlsRowPresenter(detailsPresenter) {}
        controlsRowPresenter.onActionClickedListener = OnActionClickedListener { dispatchAction(it) }
    }

    @SuppressLint("NewApi")
    private fun dispatchAction(action: Action): Boolean {
        Timber.d("dispatchAction %d", action.id)
        transportControls.apply {
            when (action.id) {
                playPauseAction.id -> if (isMediaPlaying()) pause() else play()
                rewindAction.id -> rewind()
                fastForwardAction.id -> fastForward()
                pictureInPictureAction.id -> overlayFragment.activity.enterPictureInPictureMode()
                else -> return false
            }
        }
        return true
    }

    private fun updateProgress() {
        controlsRow.currentTime = playbackState?.position?.toInt() ?: 0
        controlsRow.bufferedProgress = playbackState?.bufferedPosition?.toInt() ?: 0
    }

    private fun isMediaPlaying(): Boolean {
        if (playbackState != null) {
            val state = playbackState!!.state
            return (state == PlaybackState.STATE_BUFFERING) or
                    (state == PlaybackState.STATE_CONNECTING) or
                    (state == PlaybackState.STATE_PLAYING) or
                    (state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS) or
                    (state == PlaybackState.STATE_SKIPPING_TO_NEXT)
        }
        return false
    }

    private fun notifyItemChanged(adapter: ArrayObjectAdapter, item: Any) {
        val index = adapter.indexOf(item)
        if (index >= 0) {
            adapter.notifyArrayItemRangeChanged(index, 1)
        }
    }

    private inner class MediaControllerCallback : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState) {
            // Update your UI to reflect the new state. Do not change media playback here.
            playbackState = state
            updateProgress()
            val index = if (isMediaPlaying()) PlaybackControlsRow.PlayPauseAction.PAUSE else
                PlaybackControlsRow.PlayPauseAction.PLAY
            if (playPauseAction.index != index) {
                playPauseAction.index = index
                notifyItemChanged(actionsAdapter, playPauseAction)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata != null) {
                this@PlaybackControlsHelper.metaData = metadata
                controlsRow.totalTime = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toInt()
                overlayFragment.updatePlaybackRow()
            }
        }
    }
}