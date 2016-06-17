package com.example.dennis.proxertv.ui.player

import android.net.Uri
import android.os.Bundle
import android.support.v17.leanback.app.PlaybackOverlayFragment
import android.support.v17.leanback.widget.*
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction
import android.widget.Toast
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.model.Stream
import com.example.dennis.proxertv.ui.util.StreamPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class PlayerOverlayFragment : PlaybackOverlayFragment(), VideoPlayer.StatusListener, OnItemViewClickedListener {
    private var videoPlayer: VideoPlayer? = null
    private var seekLength = 10000 // 10 seconds, overridden once the video length is known
    private val subscriptions = CompositeSubscription()

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var actionsRow: PlaybackControlsRow
    private lateinit var actionsAdapter: ArrayObjectAdapter
    private lateinit var streamRowAdapter: ArrayObjectAdapter
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
        videoPlayer.setStatusListener(this)

        subscriptions.add(streamObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stream ->
                    if (streamRowAdapter.size() == 0) {
                        setStream(stream)
                    }
                    streamRowAdapter.add(stream)
                }, { it.printStackTrace(); checkValidStreamsFound() }, { checkValidStreamsFound() }))
    }

    private fun checkValidStreamsFound() {
        if (streamRowAdapter.size() == 0) {
            Toast.makeText(activity, "no supported streams found", Toast.LENGTH_LONG).show()
            activity.finish()
        }
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
        streamRowAdapter = ArrayObjectAdapter(StreamPresenter())
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_streams)), streamRowAdapter))

        adapter = rowsAdapter
        onItemViewClickedListener = this
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is Stream) {
            setStream(item)
        }
    }

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

    fun setStream(stream: Stream) {
        videoPlayer?.initPlayer(Uri.parse(stream.streamUrl), activity, true)
    }

    companion object {
        private const val SEEK_STEPS = 100
    }
}