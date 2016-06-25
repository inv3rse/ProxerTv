package com.inverse.unofficial.proxertv.ui.player

import android.net.Uri
import android.os.Bundle
import android.support.v17.leanback.app.PlaybackOverlayFragment
import android.support.v17.leanback.widget.*
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction
import com.google.android.exoplayer.ExoPlaybackException
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.ProxerClient
import com.inverse.unofficial.proxertv.model.Stream
import com.inverse.unofficial.proxertv.ui.util.ErrorFragment
import com.inverse.unofficial.proxertv.ui.util.StreamAdapter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class PlayerOverlayFragment : PlaybackOverlayFragment(), OnItemViewClickedListener {
    private var videoPlayer: VideoPlayer? = null
    private var seekLength = 10000 // 10 seconds, overridden once the video length is known
    private val subscriptions = CompositeSubscription()

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var actionsRow: PlaybackControlsRow
    private lateinit var actionsAdapter: ArrayObjectAdapter
    private lateinit var streamAdapter: StreamAdapter
    private lateinit var playPauseAction: PlayPauseAction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backgroundType = PlaybackOverlayFragment.BG_LIGHT
        setupActions()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    fun connectToPlayer(videoPlayer: VideoPlayer, streamObservable: Observable<Stream>) {
        this.videoPlayer = videoPlayer
        videoPlayer.setStatusListener(PlayerListener())

        subscriptions.add(streamObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stream ->
                    // add the stream to the adapter first
                    streamAdapter.addStream(stream)
                    if (streamAdapter.getCurrentStream() == null) {
                        setStream(stream)
                    }
                }, { throwable ->
                    if (throwable is ProxerClient.SeriesCaptchaException) {
                        showErrorFragment(getString(R.string.stream_captcha_error))
                    } else {
                        throwable.printStackTrace();
                        checkValidStreamsFound()
                    }
                }, { checkValidStreamsFound() }))
    }

    private fun checkValidStreamsFound() {
        if (streamAdapter.size() == 0) {
            showErrorFragment(getString(R.string.no_streams_found))
        }
    }

    private fun showErrorFragment(message: String) {
        val errorFragment = ErrorFragment.newInstance(getString(R.string.stream_error), message)
        errorFragment.dismissListener = object : ErrorFragment.ErrorDismissListener {
            override fun onDismiss() {
                activity.finish()
            }
        }

        fragmentManager.beginTransaction()
                .detach(this) // the error fragment would sometimes not keep the focus
                .add(R.id.player_root_frame, errorFragment)
                .commit()
    }

    private fun setupActions() {
        val controlsRowPresenter = PlaybackControlsRowPresenter()
        val presenterSelector = ClassPresenterSelector()
        presenterSelector.addClassPresenter(PlaybackControlsRow::class.java, controlsRowPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        rowsAdapter = ArrayObjectAdapter(presenterSelector)

        // first row (playback controls)
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

        // second row (stream selection)
        streamAdapter = StreamAdapter()
        ListRowPresenter()
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_streams)), streamAdapter))

        adapter = rowsAdapter
        onItemViewClickedListener = this
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is StreamAdapter.StreamHolder) {
            setStream(item.stream)
        }
    }

    fun setStream(stream: Stream) {
        streamAdapter.removeFailed(stream)
        streamAdapter.setCurrentStream(stream)
        videoPlayer?.initPlayer(Uri.parse(stream.streamUrl), activity, true)
    }

    /**
     * Handles all player callbacks
     */
    private inner class PlayerListener : VideoPlayer.StatusListener {
        override fun playStatusChanged(isPlaying: Boolean) {
            playPauseAction.icon = playPauseAction.getDrawable(if (isPlaying) PlayPauseAction.PAUSE else PlayPauseAction.PLAY)
            actionsAdapter.notifyArrayItemRangeChanged(actionsAdapter.indexOf(playPauseAction), 1)
        }

        override fun progressChanged(currentProgress: Long, bufferedProgress: Long) {
            actionsRow.currentTime = currentProgress.toInt()
            actionsRow.bufferedProgress = bufferedProgress.toInt()
        }

        override fun videoDurationChanged(length: Long) {
            actionsRow.totalTime = length.toInt()
            rowsAdapter.notifyArrayItemRangeChanged(rowsAdapter.indexOf(actionsRow), 1)

            seekLength = (length / SEEK_STEPS).toInt()
        }

        override fun onVideoEnd() {
            activity.finish()
        }

        override fun onError(error: ExoPlaybackException) {
            streamAdapter.getCurrentStream()?.let { currentStream ->
                streamAdapter.addFailed(currentStream)
            }
        }
    }

    companion object {
        private const val SEEK_STEPS = 100
    }
}