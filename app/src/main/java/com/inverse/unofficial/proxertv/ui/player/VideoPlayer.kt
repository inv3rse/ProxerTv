package com.inverse.unofficial.proxertv.ui.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
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
    private val mPlayer: SimpleExoPlayer
    private val mTrackSelector: DefaultTrackSelector
    private val mHandler = Handler(Looper.getMainLooper())
    private var mProgressRunnable: Runnable? = null
    private var mAspectRatio: Float = 0F

    // ui, context holding
    private var mAspectRatioFrameLayout: AspectRatioFrameLayout? = null
    private var mSurface: Surface? = null
    // might be context holding, user might have to remove it
    private var mStatusListener: StatusListener? = null

    var isInitialized: Boolean = false
        private set

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        mTrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val rendererFactory = DefaultRenderersFactory(context, null)

        mPlayer = ExoPlayerFactory.newSimpleInstance(rendererFactory, mTrackSelector)
        mPlayer.addListener(ExoPlayerListener())
        mPlayer.setVideoListener(VideoEventListener())

        if (savedState != null) {
            mAspectRatio = savedState.getFloat(KEY_ASPECT_RATIO)
            mPlayer.seekTo(savedState.getLong(KEY_PROGRESS))
        } else {
            mAspectRatio = -1f
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
            mPlayer.stop()
        }

        val dataSourceFactory = DefaultDataSourceFactory(context, USER_AGENT)
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null)

        mPlayer.prepare(videoSource, !keepPosition, !keepPosition)
        isInitialized = true
    }

    // we assume loading is the same as playing
    val isPlaying: Boolean
        get() = mPlayer.playWhenReady

    /**
     * Play once the player is ready
     */
    fun play() {
        mPlayer.playWhenReady = true
    }

    /**
     * Pause the playback
     */
    fun pause() {
        mPlayer.playWhenReady = false
    }

    /**
     * Stop the playback. To play again [initPlayer] must be called
     */
    fun stop() {
        mPlayer.stop()
        mPlayer.seekTo(0)
        mPlayer.playWhenReady = false
        isInitialized = false
    }

    /**
     * Seeks to a position
     * @param position the position in milliseconds
     */
    fun seekTo(position: Long) {
        mPlayer.seekTo(position)
        mStatusListener?.progressChanged(position, mPlayer.bufferedPosition)
    }

    val position: Long
        get() = mPlayer.currentPosition

    val bufferedPosition: Long
        get() = mPlayer.bufferedPosition

    val duration: Long
        get() = mPlayer.duration

    /**
     * Destroy the player and free used resources. The player must not be used after calling this method
     */
    fun destroy() {
        if (isInitialized) {
            mPlayer.stop()
        }

        mStatusListener = null
        disconnectFromUi()
        stopProgressUpdate()
        mPlayer.release()
    }

    /**
     * Connect the player to a surface
     * @param frameLayout the layout containing the [SurfaceView] or null
     * @param surfaceView the [SurfaceView]
     */
    fun connectToUi(frameLayout: AspectRatioFrameLayout?, surfaceView: SurfaceView) {
        Timber.d("connect to ui")

        mAspectRatioFrameLayout = frameLayout

        if (mAspectRatio != -1f) {
            frameLayout?.setAspectRatio(mAspectRatio)
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
        mPlayer.clearVideoSurface()

        mSurface = null
        mAspectRatioFrameLayout = null
    }

    /**
     * Set the [StatusListener]
     * @param listener the listener or null
     */
    fun setStatusListener(listener: StatusListener?) {
        mStatusListener = listener
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putLong(KEY_PROGRESS, mPlayer.currentPosition)
        bundle.putFloat(KEY_ASPECT_RATIO, mAspectRatio)
    }

    private fun startProgressUpdate() {
        if (mProgressRunnable == null) {
            mProgressRunnable = object : Runnable {
                override fun run() {
                    mStatusListener?.progressChanged(mPlayer.currentPosition, mPlayer.bufferedPosition)
                    mHandler.postDelayed(this, PROGRESS_UPDATE_PERIOD)
                }
            }
        }

        mHandler.post(mProgressRunnable)
    }

    private fun stopProgressUpdate() {
        if (mProgressRunnable != null) {
            mHandler.removeCallbacks(mProgressRunnable)
            mProgressRunnable = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Timber.d("surface created")
        mSurface = holder.surface
        mPlayer.setVideoSurface(mSurface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surface changed format:$format width:$width height:$height")
        // set fixed size to avoid wrong position after picture in picture
        holder.setFixedSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surface destroyed")
        if (mSurface == holder.surface) {
            disconnectFromUi()
        }
    }

    /**
     * Enable or disable all video renderer
     */
    private fun setVideoRendererDisabled(disabled: Boolean) {
        (0..(mPlayer.rendererCount - 1))
                .filter { mPlayer.getRendererType(it) == C.TRACK_TYPE_VIDEO }
                .forEach { mTrackSelector.setRendererDisabled(it, disabled) }
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

            mStatusListener?.playStatusChanged(playWhenReady, playbackState)
            mStatusListener?.videoDurationChanged(mPlayer.duration)
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onPositionDiscontinuity() {}

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

        override fun onPlayerError(error: ExoPlaybackException) {
            mStatusListener?.onError(error)
        }
    }

    private inner class VideoEventListener : SimpleExoPlayer.VideoListener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            mAspectRatio = if (height == 0) 1F else width * pixelWidthHeightRatio / height
            mAspectRatioFrameLayout?.setAspectRatio(mAspectRatio)
        }

        override fun onRenderedFirstFrame() {
        }
    }

    companion object {
        private const val KEY_PROGRESS = "KEY_PROGRESS"
        private const val KEY_ASPECT_RATIO = "KEY_ASPECT_RATIO"

        private const val USER_AGENT = "ProxerTv"
        private const val PROGRESS_UPDATE_PERIOD = 1000L
    }
}