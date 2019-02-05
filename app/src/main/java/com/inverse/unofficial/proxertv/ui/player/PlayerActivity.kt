package com.inverse.unofficial.proxertv.ui.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.ServerConfig
import com.inverse.unofficial.proxertv.model.Stream
import com.inverse.unofficial.proxertv.ui.util.ErrorState
import com.inverse.unofficial.proxertv.ui.util.LoadingState
import com.inverse.unofficial.proxertv.ui.util.SuccessState
import com.inverse.unofficial.proxertv.ui.util.extensions.provideViewModel
import com.inverse.unofficial.proxertv.ui.util.extensions.simpleListener
import kotlinx.android.synthetic.main.activity_player.*
import timber.log.Timber

/**
 * Player for an [Episode] with multiple stream options.
 */
class PlayerActivity : FragmentActivity() {

    private lateinit var overlayFragment: PlayerOverlayFragment
    private lateinit var model: PlayerViewModel
    private lateinit var videoPlayer: VideoPlayer

    private lateinit var mediaSession: MediaSession
    private lateinit var metadataBuilder: MediaMetadata.Builder

    private var seekLength = 10000 // 10 seconds, overridden once the video length is known

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        overlayFragment =
            supportFragmentManager.findFragmentById(R.id.playback_controls_fragment) as PlayerOverlayFragment

        videoPlayer = VideoPlayer(this, savedInstanceState)
        videoPlayer.setStatusListener(PlayerListener())

        val aspectFrame = findViewById<AspectRatioFrameLayout>(R.id.player_ratio_frame)
        val surfaceView = findViewById<SurfaceView>(R.id.player_surface_view)
        videoPlayer.connectToUi(aspectFrame, surfaceView)

        // create a MediaSession
        mediaSession = MediaSession(this, "ProxerTv")
        mediaSession.setCallback(MediaSessionCallback())
        mediaSession.isActive = true
        mediaController = mediaSession.controller

        model = provideViewModel(this) { App.component.getPlayerViewModel() }
        model.streams.observe(this, Observer { state ->
            when (state) {
                is LoadingState -> {
                    player_buffer_spinner.isVisible = true
                }
                is SuccessState -> {
                    val streams = state.data
                    overlayFragment.streamAdapter.setStreams(streams)

                    if (streams.isNotEmpty() && !videoPlayer.isInitialized) {
                        playStream(streams.first())
                    }
                }
                is ErrorState -> {
                    player_buffer_spinner.isVisible = false
                }
            }
        })

        initFromExtras(model)
    }

    @Suppress("DEPRECATION")
    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        if (videoPlayer.isPlaying) {
            val isVisibleBehind = requestVisibleBehind(true)
            val isInPictureInPictureMode = supportsPictureInPicture(this) && isInPictureInPictureMode
            if (!isVisibleBehind && !isInPictureInPictureMode) {
                videoPlayer.pause()
            }
        } else {
            requestVisibleBehind(false)
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun onVisibleBehindCanceled() {
        mediaController.transportControls.pause()
        super.onVisibleBehindCanceled()
    }

    override fun onDestroy() {
        mediaSession.release()
        videoPlayer.destroy()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        initFromExtras(model)
    }

    private fun playStream(stream: Stream) {
        overlayFragment.streamAdapter.removeFailed(stream)
        overlayFragment.streamAdapter.setCurrentStream(stream)

        videoPlayer.initPlayer(Uri.parse(stream.streamUrl), this, false)
        videoPlayer.play()
    }

    /**
     * Initializes episode based on intent extras
     */
    private fun initFromExtras(model: PlayerViewModel) {
        val seriesExtra = intent.getParcelableExtra<Series>(PlayerActivity.EXTRA_SERIES)
            ?: throw IllegalArgumentException("series extra must not be null")
        val episodeExtra = intent.getParcelableExtra<Episode>(PlayerActivity.EXTRA_EPISODE)
            ?: throw IllegalArgumentException("episode extra must not be null")

        overlayFragment.streamAdapter.clear()

        initMediaMetadata(seriesExtra, episodeExtra)
        setPendingIntent(seriesExtra, episodeExtra)
        model.init(seriesExtra, episodeExtra)
    }

    private fun initMediaMetadata(series: Series, episode: Episode) {
        val episodeText = getString(R.string.episode, episode.episodeNum)
        metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, series.name)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, episodeText)
            .putString(MediaMetadata.METADATA_KEY_TITLE, series.name)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, episodeText)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, 0L)

        Glide.with(this)
            .asBitmap()
            .load(ServerConfig.coverUrl(series.id))
            .simpleListener(
                onLoadFailed = {
                    mediaSession.setMetadata(metadataBuilder.build())
                },
                onResourceReady = { resource ->
                    if (resource != null) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, resource)
                    }
                    mediaSession.setMetadata(metadataBuilder.build())
                })
            .submit()
    }


    private fun setPendingIntent(series: Series, episode: Episode) {
        val intent = createIntent(this, series, episode)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mediaSession.setSessionActivity(pi)
    }

    /**
     * Handles controls from MediaSession
     */
    private inner class MediaSessionCallback : MediaSession.Callback() {

        override fun onPlay() {
            videoPlayer.play()
        }

        override fun onPause() {
            videoPlayer.pause()
        }

        override fun onFastForward() {
            videoPlayer.seekTo(videoPlayer.position + seekLength)
        }

        override fun onRewind() {
            videoPlayer.apply { seekTo(if (position > seekLength) position - seekLength else 0) }
        }

        override fun onSeekTo(position: Long) {
            videoPlayer.seekTo(position)
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            if (action != CustomPlayerAction.PLAY_STREAM) {
                return
            }

            val url = extras?.getString(CustomPlayerAction.KEY_STREAM_URL) ?: return
            val providerName = extras.getString(CustomPlayerAction.KEY_PROVIDER_NAME) ?: return
            playStream(Stream(url, providerName))
        }
    }

    private fun setPlaybackState(state: Int) {
        val playbackState = PlaybackState.Builder()
            .setState(state, videoPlayer.position, 1F)
            .setBufferedPosition(videoPlayer.bufferedPosition)
            .setActions(getStateActions(state))
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun getStateActions(state: Int): Long {
        val playPause =
            if (state == PlaybackState.STATE_PLAYING) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY

        return playPause or PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_REWIND
    }

    /**
     * Handles all player callbacks
     */
    private inner class PlayerListener : VideoPlayer.StatusListener {
        private var state: Int = PlaybackState.STATE_NONE
        private var isTracked = false

        override fun playStatusChanged(isPlaying: Boolean, playbackState: Int) {
            state = when (playbackState) {
                ExoPlayer.STATE_IDLE -> PlaybackState.STATE_NONE
                ExoPlayer.STATE_BUFFERING -> if (isPlaying)
                    PlaybackState.STATE_BUFFERING else
                    PlaybackState.STATE_PAUSED
                ExoPlayer.STATE_READY -> if (isPlaying)
                    PlaybackState.STATE_PLAYING else
                    PlaybackState.STATE_PAUSED
                ExoPlayer.STATE_ENDED -> PlaybackState.STATE_STOPPED
                else -> PlaybackState.STATE_NONE
            }

            player_buffer_spinner.isVisible = playbackState == ExoPlayer.STATE_BUFFERING && isPlaying

            if (playbackState == ExoPlayer.STATE_ENDED) {
                finish()
            } else {
                setPlaybackState(state)
            }
        }

        override fun progressChanged(currentProgress: Long, bufferedProgress: Long) {
            setPlaybackState(state)
            if (!isTracked && videoPlayer.duration > 0) {
                val progressPercent = currentProgress.toFloat() / videoPlayer.duration
                if (progressPercent > TRACK_PERCENT) {
                    // track episode
                    isTracked = true
                    model.markAsWatched()
                }
            }
        }

        override fun videoDurationChanged(length: Long) {
            seekLength = (length / SEEK_STEPS).toInt()
            metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, length)
            mediaSession.setMetadata(metadataBuilder.build())
        }

        override fun onError(error: ExoPlaybackException) {
            error.printStackTrace()
            overlayFragment.streamAdapter.getCurrentStream()?.let { currentStream ->
                overlayFragment.streamAdapter.addFailed(currentStream)
            }
        }
    }

    companion object {
        private const val SEEK_STEPS = 100
        private const val TRACK_PERCENT = 0.75F

        private const val EXTRA_EPISODE = "EXTRA_EPISODE"
        private const val EXTRA_SERIES = "EXTRA_SERIES"

        fun createIntent(context: Context, series: Series, episode: Episode): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(EXTRA_SERIES, series)
            intent.putExtra(EXTRA_EPISODE, episode)

            return intent
        }

        fun supportsPictureInPicture(context: Context): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            )
        }
    }
}

