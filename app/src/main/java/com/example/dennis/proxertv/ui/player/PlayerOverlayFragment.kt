package com.example.dennis.proxertv.ui.player

import android.os.Bundle
import android.support.v17.leanback.app.PlaybackOverlayFragment
import android.support.v17.leanback.widget.*
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction

class PlayerOverlayFragment : PlaybackOverlayFragment(), VideoPlayer.StatusListener {
    private var videoPlayer: VideoPlayer? = null

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var actionsRow: PlaybackControlsRow
    private lateinit var actionsAdapter: ArrayObjectAdapter
    private lateinit var playPauseAction: PlayPauseAction

    private var seekLength = 10000 // 10 seconds, overridden once the video length is known

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backgroundType = PlaybackOverlayFragment.BG_LIGHT
        setupActions()
    }

    fun connectToPlayer(videoPlayer: VideoPlayer) {
        this.videoPlayer = videoPlayer
        videoPlayer.setStatusListener(this)
    }

    private fun setupActions() {
        val controlsRowPresenter = PlaybackControlsRowPresenter()
        rowsAdapter = ArrayObjectAdapter(controlsRowPresenter)

        actionsRow = PlaybackControlsRow()
        actionsAdapter = ArrayObjectAdapter(ControlButtonPresenterSelector())

        actionsRow.primaryActionsAdapter = actionsAdapter

        playPauseAction = PlaybackControlsRow.PlayPauseAction(activity)
        val rewindAction = PlaybackControlsRow.RewindAction(activity)
        val fastForwardAction = PlaybackControlsRow.FastForwardAction(activity)

        actionsAdapter.add(rewindAction)
        actionsAdapter.add(playPauseAction)
        actionsAdapter.add(fastForwardAction)

        rowsAdapter.add(actionsRow)
        controlsRowPresenter.onActionClickedListener = OnActionClickedListener { action ->
            videoPlayer?.apply {
                when (action.id) {
                    playPauseAction.id -> if (isPlaying) pause() else play()
                    rewindAction.id -> seekTo(if (position > seekLength) position - seekLength else 0)
                    fastForwardAction.id -> seekTo(position + seekLength)
                }
            }
        }

        adapter = rowsAdapter
    }

    override fun playStatusChanged(isPlaying: Boolean) {
        playPauseAction.icon = playPauseAction.getDrawable(if (isPlaying) PlayPauseAction.PAUSE else PlayPauseAction.PLAY)
        actionsAdapter.notifyArrayItemRangeChanged(actionsAdapter.indexOf(playPauseAction), 1)
    }

    override fun progressChanged(currentProgress: Long) {
        actionsRow.currentTime = currentProgress.toInt()
    }

    override fun videoDurationChanged(length: Long) {
        actionsRow.totalTime = length.toInt()
        rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(actionsRow), 1)

        seekLength = (length / SEEK_STEPS).toInt()
    }

    companion object {
        private const val SEEK_STEPS = 150
    }
}