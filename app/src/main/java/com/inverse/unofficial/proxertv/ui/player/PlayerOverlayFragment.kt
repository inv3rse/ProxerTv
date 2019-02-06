package com.inverse.unofficial.proxertv.ui.player

import android.media.session.MediaController
import android.os.Bundle
import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.widget.*
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.ui.util.StreamAdapter

/**
 * We use this fragment as an implementation detail of the PlayerActivity.
 * Therefore it is somewhat strongly tied to it.
 */
class PlayerOverlayFragment : PlaybackSupportFragment(), OnItemViewClickedListener {

    lateinit var streamAdapter: StreamAdapter
    private lateinit var playbackControlsHelper: PlaybackControlsHelper

    private lateinit var mediaControllerCallback: MediaController.Callback
    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        streamAdapter = StreamAdapter()

        backgroundType = PlaybackSupportFragment.BG_LIGHT
        isControlsOverlayAutoHideEnabled = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity()

        // connect session to controls
        playbackControlsHelper = PlaybackControlsHelper(activity, this)
        mediaControllerCallback = playbackControlsHelper.createMediaControllerCallback()
        activity.mediaController.registerCallback(mediaControllerCallback)

        setupAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.mediaController?.unregisterCallback(mediaControllerCallback)
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        when (item) {
            is StreamAdapter.StreamHolder -> {
                requireActivity().mediaController.transportControls.playStream(item.stream)
            }
        }
    }

    fun updatePlaybackRow() {
        rowsAdapter.notifyArrayItemRangeChanged(0, 1)
    }

    private fun setupAdapter() {
        val controlsRowPresenter = playbackControlsHelper.controlsRowPresenter
        val presenterSelector = ClassPresenterSelector()

        presenterSelector.addClassPresenter(PlaybackControlsRow::class.java, controlsRowPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        rowsAdapter = ArrayObjectAdapter(presenterSelector)

        // first row (playback controls)
        rowsAdapter.add(playbackControlsHelper.controlsRow)

        // second row (stream selection)
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_streams)), streamAdapter))

        updatePlaybackRow()

        adapter = rowsAdapter
        setOnItemViewClickedListener(this)
    }
}