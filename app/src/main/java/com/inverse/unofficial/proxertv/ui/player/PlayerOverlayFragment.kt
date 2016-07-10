package com.inverse.unofficial.proxertv.ui.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.support.v17.leanback.app.PlaybackOverlayFragment
import android.support.v17.leanback.widget.*
import android.view.SurfaceView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.google.android.exoplayer.AspectRatioFrameLayout
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.ExoPlayer
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.ProxerClient
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.Stream
import com.inverse.unofficial.proxertv.ui.util.ErrorFragment
import com.inverse.unofficial.proxertv.ui.util.StreamAdapter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

/**
 * We use this fragment as an implementation detail of the PlayerActivity.
 * Therefore it is somewhat strongly tied to it.
 */
class PlayerOverlayFragment : PlaybackOverlayFragment(), OnItemViewClickedListener {
    private var seekLength = 10000 // 10 seconds, overridden once the video length is known
    private val subscriptions = CompositeSubscription()
    private lateinit var playbackControlsHelper: PlaybackControlsHelper

    private lateinit var videoPlayer: VideoPlayer
    private lateinit var mediaControllerCallback: MediaController.Callback
    private lateinit var mediaSession: MediaSession
    private lateinit var audioManager: AudioManager

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var streamAdapter: StreamAdapter

    private lateinit var metadataBuilder: MediaMetadata.Builder
    private var episode: Episode? = null
    private var series: Series? = null

    private var hasAudioFocus: Boolean = false
    private var pauseTransient: Boolean = false
    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                abandonAudioFocus()
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (videoPlayer.isPlaying) {
                pause()
                pauseTransient = true
            }
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                if (pauseTransient) {
                    play()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // create a MediaSession
        mediaSession = MediaSession(activity, "ProxerTv")
        mediaSession.setCallback(MediaSessionCallback())
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.isActive = true

        videoPlayer = VideoPlayer(savedInstanceState)
        videoPlayer.setStatusListener(PlayerListener())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("onActivityCreated")

        activity.mediaController = MediaController(activity, mediaSession.sessionToken)

        // connect session to controls
        playbackControlsHelper = PlaybackControlsHelper(activity, this)
        mediaControllerCallback = playbackControlsHelper.createMediaControllerCallback()
        activity.mediaController.registerCallback(mediaControllerCallback)

        backgroundType = PlaybackOverlayFragment.BG_LIGHT
        setupAdapter()

        initEpisode()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")

        val surfaceView = activity.findViewById(R.id.player_surface_view) as SurfaceView
        val aspectFrame = activity.findViewById(R.id.player_ratio_frame) as AspectRatioFrameLayout

        videoPlayer.connectToUi(aspectFrame, surfaceView)
    }

    @SuppressLint("NewApi")
    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        if (videoPlayer.isPlaying) {
            val isVisibleBehind = activity.requestVisibleBehind(true)
            val isInPictureInPictureMode = PlayerActivity.supportsPictureInPicture(activity) and
                    activity.isInPictureInPictureMode
            if (!isVisibleBehind && !isInPictureInPictureMode) {
                pause()
            }
        } else {
            activity.requestVisibleBehind(false)
        }
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        activity.mediaController.unregisterCallback(mediaControllerCallback)
        videoPlayer.destroy()
        abandonAudioFocus()
        mediaSession.release()
        subscriptions.clear()
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        when (item) {
            is StreamAdapter.StreamHolder -> setStream(item.stream)
        }
    }

    /**
     * Initializes episode based on intent extras
     */
    fun initEpisode() {
        val extras = activity.intent.extras
        val episodeExtra = extras.getParcelable<Episode>(PlayerActivity.EXTRA_EPISODE)
        val seriesExtra = extras.getParcelable<Series>(PlayerActivity.EXTRA_SERIES)
        if ((episodeExtra != null) and (seriesExtra != null)) {
            if ((episodeExtra != episode) or (seriesExtra != series)) {
                episode = episodeExtra
                series = seriesExtra

                if (videoPlayer.isInitialized) {
                    videoPlayer.stop()
                }

                initMediaMetadata()
                setPendingIntent()
                loadStreams()
            }
        } else {
            Timber.d("missing extras, finishing!")
            activity.finish()
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
        streamAdapter = StreamAdapter()
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_streams)), streamAdapter))

        updatePlaybackRow()

        adapter = rowsAdapter
        onItemViewClickedListener = this
    }

    private fun loadStreams() {
        val client = App.component.getProxerClient()
        streamAdapter.clear()

        subscriptions.add(client.loadEpisodeStreams(episode!!.seriesId, episode!!.episodeNum, episode!!.languageType)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
                        throwable.printStackTrace()
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

    private fun setStream(stream: Stream) {
        streamAdapter.removeFailed(stream)
        streamAdapter.setCurrentStream(stream)
        videoPlayer.initPlayer(Uri.parse(stream.streamUrl), activity, true)
        play()
    }

    private fun play() {
        requestAudioFocus()
        if (hasAudioFocus) {
            videoPlayer.play()
        }
    }

    private fun pause() {
        pauseTransient = false
        videoPlayer.pause()
    }

    private fun initMediaMetadata() {
        val episodeText = getString(R.string.episode, episode!!.episodeNum)
        metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, series!!.englishTitle)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, episodeText)
                .putString(MediaMetadata.METADATA_KEY_TITLE, series!!.englishTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, episodeText)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 0L)

        Glide.with(this).load(episode!!.coverUrl).asBitmap().into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(bitmap: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)
                mediaSession.setMetadata(metadataBuilder.build())
            }
        })
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) {
            return
        }
        val result = audioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasAudioFocus = true
        } else {
            pause()
        }
    }

    private fun abandonAudioFocus() {
        hasAudioFocus = false
        audioManager.abandonAudioFocus(mOnAudioFocusChangeListener)
    }

    private fun setPlaybackState(state: Int) {
        val playbackState = PlaybackState.Builder()
                .setState(state, videoPlayer.position, 1F)
                .setBufferedPosition(videoPlayer.bufferedPosition)
                .setActions(getStateActions(state))
                .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun setPendingIntent() {
        val intent = Intent(activity.intent)
        intent.putExtra(PlayerActivity.EXTRA_EPISODE, episode)

        val pi = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mediaSession.setSessionActivity(pi)
    }

    private fun getStateActions(state: Int): Long {
        return (if (state == PlaybackState.STATE_PLAYING)
            PlaybackState.ACTION_PAUSE else
            PlaybackState.ACTION_PLAY) or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND
    }

    /**
     * Handles controls from MediaSession
     */
    private inner class MediaSessionCallback : MediaSession.Callback() {

        override fun onPlay() {
            play()
            tickle()
        }

        override fun onPause() {
            pause()
            tickle()
        }

        override fun onFastForward() {
            videoPlayer.seekTo(videoPlayer.position + seekLength)
            tickle()
        }

        override fun onRewind() {
            videoPlayer.apply { seekTo(if (position > seekLength) position - seekLength else 0) }
            tickle()
        }

        override fun onSeekTo(position: Long) {
            videoPlayer.seekTo(position)
            tickle()
        }
    }

    /**
     * Handles all player callbacks
     */
    private inner class PlayerListener : VideoPlayer.StatusListener {
        private var state: Int = PlaybackState.STATE_NONE

        override fun playStatusChanged(isPlaying: Boolean, playbackState: Int) {
            state = when (playbackState) {
                ExoPlayer.STATE_IDLE -> PlaybackState.STATE_NONE
                ExoPlayer.STATE_PREPARING -> PlaybackState.STATE_CONNECTING
                ExoPlayer.STATE_BUFFERING -> if (isPlaying)
                    PlaybackState.STATE_BUFFERING else
                    PlaybackState.STATE_PAUSED
                ExoPlayer.STATE_READY -> if (isPlaying)
                    PlaybackState.STATE_PLAYING else
                    PlaybackState.STATE_PAUSED
                ExoPlayer.STATE_ENDED -> PlaybackState.STATE_STOPPED
                else -> PlaybackState.STATE_NONE
            }

            if (playbackState == ExoPlayer.STATE_ENDED) {
                activity.finish()
            } else {
                setPlaybackState(state)
            }
        }

        override fun progressChanged(currentProgress: Long, bufferedProgress: Long) {
            setPlaybackState(state)
        }

        override fun videoDurationChanged(length: Long) {
            seekLength = (length / SEEK_STEPS).toInt()
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, length)
            mediaSession.setMetadata(metadataBuilder.build())
        }

        override fun onError(error: ExoPlaybackException) {
            error.printStackTrace()
            streamAdapter.getCurrentStream()?.let { currentStream ->
                streamAdapter.addFailed(currentStream)
            }
        }
    }

    companion object {
        private const val SEEK_STEPS = 100
    }
}