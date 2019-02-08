package com.inverse.unofficial.proxertv.ui.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import timber.log.Timber

/**
 * A simple video player based on the [SimpleExoPlayer].
 */
class VideoPlayer(context: Context, savedState: Bundle? = null) : SurfaceHolder.Callback {

    private val player: SimpleExoPlayer
    private val trackSelector: DefaultTrackSelector
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var aspectRatio: Float = 0F

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var hasAudioFocus: Boolean = false

    // ui, context holding
    private var aspectRatioFrameLayout: AspectRatioFrameLayout? = null
    private var surface: Surface? = null
    // might be context holding, user might have to remove it
    private var statusListener: StatusListener? = null

    private var audioFocusRequest: AudioFocusRequestCompat? = null
    private val audioFocusChangeListener = AudioFocusListener()

    var isInitialized: Boolean = false
        private set

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val rendererFactory = DefaultRenderersFactory(context, null)

        player = ExoPlayerFactory.newSimpleInstance(rendererFactory, trackSelector)
        player.addListener(ExoPlayerListener())
        player.setVideoListener(VideoEventListener())

        if (savedState != null) {
            aspectRatio = savedState.getFloat(KEY_ASPECT_RATIO)
            player.seekTo(savedState.getLong(KEY_PROGRESS))
        } else {
            aspectRatio = -1f
        }
    }

    /**
     * Initialize the player to play to play a video
     * @param videoUri the uri of the video to play
     * @param context a context
     * @param keepPosition if set to true the player will keep the current position
     */
    fun initPlayer(videoUri: Uri, context: Context, keepPosition: Boolean = false) {
        if (isInitialized) {
            player.stop()
        }

        val dataSourceFactory = DefaultDataSourceFactory(context, USER_AGENT)
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null)

        player.prepare(videoSource, !keepPosition, !keepPosition)
        isInitialized = true
    }

    // we assume loading is the same as playing
    val isPlaying: Boolean
        get() = player.playWhenReady

    /**
     * Play once the player is ready
     */
    fun play() {
        if (requestAudioFocus()) {
            player.playWhenReady = true
        }
    }

    /**
     * Pause the playback
     */
    fun pause() {
        player.playWhenReady = false
    }

    /**
     * Stop the playback. To play again [initPlayer] must be called
     */
    fun stop() {
        player.stop()
        player.seekTo(0)
        player.playWhenReady = false
        abandonAudioFocus()
        isInitialized = false
    }

    /**
     * Seeks to a position
     * @param position the position in milliseconds
     */
    fun seekTo(position: Long) {
        player.seekTo(position)
        statusListener?.progressChanged(position, player.bufferedPosition)
    }

    val position: Long
        get() = player.currentPosition

    val bufferedPosition: Long
        get() = player.bufferedPosition

    val duration: Long
        get() = player.duration

    /**
     * Destroy the player and free used resources. The player must not be used after calling this method
     */
    fun destroy() {
        if (isInitialized) {
            stop()
        }

        statusListener = null
        disconnectFromUi()
        stopProgressUpdate()
        player.release()
    }

    /**
     * Connect the player to a surface
     * @param frameLayout the layout containing the [SurfaceView] or null
     * @param surfaceView the [SurfaceView]
     */
    fun connectToUi(frameLayout: AspectRatioFrameLayout?, surfaceView: SurfaceView) {
        Timber.d("connect to ui")

        aspectRatioFrameLayout = frameLayout

        if (aspectRatio != -1f) {
            frameLayout?.setAspectRatio(aspectRatio)
        }

        setVideoRendererDisabled(false)
        surfaceView.holder.removeCallback(this)
        surfaceView.holder.addCallback(this)
    }

    /**
     * Disconnect the player from its ui components
     */
    fun disconnectFromUi() {
        Timber.d("disconnect from ui")

        setVideoRendererDisabled(true)
        player.clearVideoSurface()

        surface = null
        aspectRatioFrameLayout = null
    }

    /**
     * Set the [StatusListener]
     * @param listener the listener or null
     */
    fun setStatusListener(listener: StatusListener?) {
        statusListener = listener
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putLong(KEY_PROGRESS, player.currentPosition)
        bundle.putFloat(KEY_ASPECT_RATIO, aspectRatio)
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            return true
        }

        val focusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()


        val result = AudioManagerCompat.requestAudioFocus(audioManager, focusRequest)
        return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasAudioFocus = true
            audioFocusRequest = focusRequest
            true
        } else {
            false
        }
    }

    private fun abandonAudioFocus() {
        hasAudioFocus = false
        audioFocusRequest?.let {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
        }
        audioFocusRequest = null
    }

    private fun startProgressUpdate() {
        if (progressRunnable == null) {
            progressRunnable = object : Runnable {
                override fun run() {
                    statusListener?.progressChanged(player.currentPosition, player.bufferedPosition)
                    mainHandler.postDelayed(this, PROGRESS_UPDATE_PERIOD)
                }
            }
            mainHandler.post(progressRunnable)
        }
    }

    private fun stopProgressUpdate() {
        if (progressRunnable != null) {
            mainHandler.removeCallbacks(progressRunnable)
            progressRunnable = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("surface created")
        surface = holder.surface
        player.setVideoSurface(surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surface changed format:$format width:$width height:$height")
        // set fixed size to avoid wrong position after picture in picture
        holder.setFixedSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surface destroyed")
        if (surface == holder.surface) {
            disconnectFromUi()
        }
    }

    /**
     * Enable or disable all video renderer
     */
    private fun setVideoRendererDisabled(disabled: Boolean) {
        (0..(player.rendererCount - 1))
            .filter { player.getRendererType(it) == C.TRACK_TYPE_VIDEO }
            .forEach { trackSelector.setRendererDisabled(it, disabled) }
    }

    interface StatusListener {
        fun playStatusChanged(isPlaying: Boolean, playbackState: Int)

        fun progressChanged(currentProgress: Long, bufferedProgress: Long)

        fun videoDurationChanged(length: Long)

        fun onError(error: ExoPlaybackException)
    }

    private inner class ExoPlayerListener : ExoPlayer.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }

            statusListener?.playStatusChanged(playWhenReady, playbackState)
            statusListener?.videoDurationChanged(player.duration)
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onPositionDiscontinuity() {}

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

        override fun onPlayerError(error: ExoPlaybackException) {
            statusListener?.onError(error)
        }
    }

    private inner class VideoEventListener : SimpleExoPlayer.VideoListener {
        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
            aspectRatio = if (height == 0) 1F else width * pixelWidthHeightRatio / height
            aspectRatioFrameLayout?.setAspectRatio(aspectRatio)
        }

        override fun onRenderedFirstFrame() {
        }
    }

    private inner class AudioFocusListener : AudioManager.OnAudioFocusChangeListener {
        private var pauseTransient: Boolean = false

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    abandonAudioFocus()
                    pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying) {
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
    }

    companion object {
        private const val KEY_PROGRESS = "KEY_PROGRESS"
        private const val KEY_ASPECT_RATIO = "KEY_ASPECT_RATIO"

        private const val USER_AGENT = "ProxerTv"
        private const val PROGRESS_UPDATE_PERIOD = 1000L
    }
}