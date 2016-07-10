package com.inverse.unofficial.proxertv.ui.player

import android.content.Context
import android.media.MediaCodec
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.exoplayer.*
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultHttpDataSource
import timber.log.Timber

class VideoPlayer(savedState: Bundle? = null) : SurfaceHolder.Callback {
    private val mPlayer: ExoPlayer
    private var mVideoRenderer: TrackRenderer? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var mProgressRunnable: Runnable? = null
    private var mAspectRatio: Float = 0F
    private var mSelectedVideoTrack = -1

    // ui, context holding
    private var mAspectRatioFrameLayout: AspectRatioFrameLayout? = null
    private var mSurface: Surface? = null
    // might be context holding, user might have to remove it
    private var mStatusListener: StatusListener? = null

    var isInitialized: Boolean = false
        private set

    init {
        mPlayer = ExoPlayer.Factory.newInstance(2)
        mPlayer.addListener(ExoPlayerListener())

        if (savedState != null) {
            mAspectRatio = savedState.getFloat(KEY_ASPECT_RATIO)
            mPlayer.seekTo(savedState.getLong(KEY_PROGRESS))
        } else {
            mAspectRatio = -1f
        }
    }

    fun initPlayer(videoUri: Uri, context: Context, keepPosition: Boolean = false) {
        if (isInitialized) {
            mPlayer.stop()
            if (!keepPosition) {
                mPlayer.seekTo(0L)
            }
        }

        val sampleSource = ExtractorSampleSource(videoUri, DefaultHttpDataSource(USER_AGENT, null),
                DefaultAllocator(BUFFER_SEGMENT_SIZE), BUFFER_VIDEO_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE)

        mVideoRenderer = MediaCodecVideoTrackRenderer(context, sampleSource,
                MediaCodecSelector.DEFAULT, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 0,
                Handler(Looper.getMainLooper()), VideoTrackEventListener(), -1)

        val audioTrackRenderer = MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT)
        mPlayer.prepare(mVideoRenderer, audioTrackRenderer)

        isInitialized = true
        if (mSurface != null) {
            pushSurface(mSurface, false)
        }
    }

    // we assume loading is the same as playing
    val isPlaying: Boolean
        get() = mPlayer.playWhenReady

    fun play() {
        mPlayer.playWhenReady = true
    }

    fun pause() {
        mPlayer.playWhenReady = false
    }

    fun stop() {
        mPlayer.stop()
        mPlayer.seekTo(0)
        mPlayer.playWhenReady = false
        isInitialized = false
    }

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

    fun destroy() {
        if (isInitialized) {
            mPlayer.stop()
        }

        mStatusListener = null
        disconnectFromUi()
        stopProgressUpdate()
        mPlayer.release()
    }

    fun connectToUi(frameLayout: AspectRatioFrameLayout, surfaceView: SurfaceView) {
        Timber.d("connect to ui")

        mAspectRatioFrameLayout = frameLayout

        if (mAspectRatio != -1f) {
            frameLayout.setAspectRatio(mAspectRatio)
        }

        mSurface = surfaceView.holder.surface
        surfaceView.holder.addCallback(this)

        if (isInitialized) {
            pushSurface(mSurface, true)
            if (mSelectedVideoTrack >= 0) {
                mPlayer.setSelectedTrack(VIDEO_RENDERER, mSelectedVideoTrack)
            }
        }
    }

    fun disconnectFromUi() {
        Timber.d("disconnect from ui")

        if (mSurface != null) {
            pushSurface(null, true)
        }

        val track = mPlayer.getSelectedTrack(VIDEO_RENDERER)
        if (track >= 0) {
            mSelectedVideoTrack = track
        }

        mPlayer.setSelectedTrack(VIDEO_RENDERER, -1)

        mSurface = null
        mAspectRatioFrameLayout = null
    }

    fun setStatusListener(listener: StatusListener?) {
        mStatusListener = listener
    }

    fun saveInstanceState(bundle: Bundle) {
        bundle.putLong(KEY_PROGRESS, mPlayer.currentPosition)
        bundle.putFloat(KEY_ASPECT_RATIO, mAspectRatio)
    }

    private fun pushSurface(surface: Surface?, blocking: Boolean) {
        if (blocking) {
            mPlayer.blockingSendMessage(mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface)
        } else {
            mPlayer.sendMessage(mVideoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface)
        }
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
        if (mSurface !== holder.surface) {
            pushSurface(holder.surface, false)
            mSurface = holder.surface
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Timber.d("surface changed")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Timber.d("surface destroyed")
        if (holder.surface === mSurface) {
            disconnectFromUi()
        }
    }

    interface StatusListener {
        fun playStatusChanged(isPlaying: Boolean, playbackState: Int)

        fun progressChanged(currentProgress: Long, bufferedProgress: Long)

        fun videoDurationChanged(length: Long)

        fun onError(error: ExoPlaybackException)
    }

    private inner class ExoPlayerListener : ExoPlayer.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }

            mStatusListener?.playStatusChanged(playWhenReady, playbackState)
            mStatusListener?.videoDurationChanged(mPlayer.duration)
        }

        override fun onPlayWhenReadyCommitted() {

        }

        override fun onPlayerError(error: ExoPlaybackException) {
            mStatusListener?.onError(error)
        }
    }

    private inner class VideoTrackEventListener : MediaCodecVideoTrackRenderer.EventListener {

        override fun onDroppedFrames(count: Int, elapsed: Long) {

        }

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            if (mAspectRatioFrameLayout != null) {
                mAspectRatio = if (height == 0) 1F else width * pixelWidthHeightRatio / height
                mAspectRatioFrameLayout!!.setAspectRatio(mAspectRatio)
            }
        }

        override fun onDrawnToSurface(surface: Surface) {

        }

        override fun onDecoderInitializationError(e: MediaCodecTrackRenderer.DecoderInitializationException) {

        }

        override fun onCryptoError(e: MediaCodec.CryptoException) {

        }

        override fun onDecoderInitialized(decoderName: String, elapsedRealtimeMs: Long, initializationDurationMs: Long) {

        }
    }

    companion object {
        private const val KEY_PROGRESS = "KEY_PROGRESS"
        private const val KEY_ASPECT_RATIO = "KEY_ASPECT_RATIO"

        private const val USER_AGENT = "ProxerTv"
        private const val BUFFER_SEGMENT_SIZE = 64 * 1024
        private const val BUFFER_VIDEO_SEGMENT_COUNT = 160
        private const val PROGRESS_UPDATE_PERIOD = 1000L

        private const val VIDEO_RENDERER = 0
    }
}