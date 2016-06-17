package com.inverse.unofficial.proxertv.ui.player

import android.content.Context
import android.media.MediaCodec
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.exoplayer.*
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultHttpDataSource

class VideoPlayer(savedState: Bundle? = null) : SurfaceHolder.Callback {

    private val mPlayer: ExoPlayer
    private var mVideoRenderer: TrackRenderer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var mAspectRatio: Float = 0F

    // ui, context holding
    private var mAspectRatioFrameLayout: AspectRatioFrameLayout? = null
    private var mSurface: Surface? = null
    // might be context holding, user might have to remove it
    private var mStatusListener: StatusListener? = null

    var progressUpdatePeriod: Long = 1000
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

    fun seekTo(position: Long) {
        mPlayer.seekTo(position)
        mStatusListener?.progressChanged(position, mPlayer.bufferedPosition)
    }

    val position: Long
        get() = mPlayer.currentPosition

    val duration: Long
        get() = mPlayer.duration

    fun destroy() {
        if (isInitialized) {
            mPlayer.stop()
        }

        disconnectFromUi()
        stopProgressUpdate()
        mPlayer.release()
    }

    fun connectToUi(frameLayout: AspectRatioFrameLayout, surfaceView: SurfaceView) {
        Log.d(TAG, "connect to ui")

        mAspectRatioFrameLayout = frameLayout

        if (mAspectRatio != -1f) {
            frameLayout.setAspectRatio(mAspectRatio)
        }

        mSurface = surfaceView.holder.surface
        surfaceView.holder.addCallback(this)

        if (isInitialized) {
            pushSurface(mSurface, true)
        }
    }

    fun disconnectFromUi() {
        Log.d(TAG, "disconnect from ui")

        if (mSurface != null) {
            pushSurface(null, true)
        }

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
        if (progressRunnable == null) {
            progressRunnable = object : Runnable {
                override fun run() {
                    mStatusListener?.progressChanged(mPlayer.currentPosition, mPlayer.bufferedPosition)
                    handler.postDelayed(this, progressUpdatePeriod)
                }
            }
        }

        handler.post(progressRunnable)
    }

    private fun stopProgressUpdate() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable)
            progressRunnable = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (mSurface !== holder.surface) {
            pushSurface(holder.surface, false)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (holder.surface === mSurface) {
            disconnectFromUi()
        }
    }

    interface StatusListener {
        fun playStatusChanged(isPlaying: Boolean)

        fun progressChanged(currentProgress: Long, bufferedProgress: Long)

        fun videoDurationChanged(length: Long)

        fun onVideoEnd()
    }

    private inner class ExoPlayerListener : ExoPlayer.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playWhenReady) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }

            mStatusListener?.playStatusChanged(playWhenReady)
            mStatusListener?.videoDurationChanged(mPlayer.duration)

            if (playbackState == ExoPlayer.STATE_ENDED) {
                mStatusListener?.onVideoEnd()
            }
        }

        override fun onPlayWhenReadyCommitted() {

        }

        override fun onPlayerError(error: ExoPlaybackException) {

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

        private const val TAG = "VideoPlayer"
    }
}