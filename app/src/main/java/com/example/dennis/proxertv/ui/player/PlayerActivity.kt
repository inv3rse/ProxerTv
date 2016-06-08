package com.example.dennis.proxertv.ui.player

import android.app.Activity
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceView
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.base.App
import com.google.android.exoplayer.AspectRatioFrameLayout
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class PlayerActivity : Activity() {

    private val videoPlayer = VideoPlayer()

    private lateinit var surfaceView: SurfaceView
    private lateinit var aspectFrame: AspectRatioFrameLayout
    private lateinit var mediaSession: MediaSession

    private var streamUrls = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        surfaceView = findViewById(R.id.player_surface_view) as SurfaceView
        aspectFrame = findViewById(R.id.player_ratio_frame) as AspectRatioFrameLayout

        videoPlayer.connectToUi(aspectFrame, surfaceView)

        mediaSession = MediaSession(this, "ProxerTv")
        mediaSession.setCallback(object : MediaSession.Callback() {})
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS and MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.isActive = true

        loadStreamUrl()

        val fragment = fragmentManager.findFragmentById(R.id.playback_controls_fragment) as PlayerOverlayFragment
        fragment.connectToPlayer(videoPlayer)
    }

    override fun onStart() {
        super.onStart()
        videoPlayer.play()
    }

    override fun onStop() {
        super.onStop()
        videoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayer.destroy()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                videoPlayer.play()
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                videoPlayer.pause()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (videoPlayer.isPlaying) {
                    videoPlayer.pause()
                } else {
                    videoPlayer.play()
                }
            }
            else -> return super.onKeyUp(keyCode, event)
        }

        return true
    }

    private fun loadStreamUrl() {
        val client = App.component.getProxerClient()

        val id = intent.extras.getInt(EXTRA_SERIES_ID)
        val episode = intent.extras.getInt(EXTRA_EPISODE_NUM)
        val lang = intent.extras.getString(EXTRA_LANG_TYPE)

        client.loadEpisodeStreams(id, episode, lang)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    streamUrls.add(it)
                    if (!videoPlayer.isInitialized) {
                        setStream(it)
                    }
                }, { it.printStackTrace() }, {})
    }

    fun setStream(url: String) {
        videoPlayer.initPlayer(Uri.parse(url), this)
    }

    companion object {
        const val EXTRA_SERIES_ID = "EXTRA_SERIES_ID"
        const val EXTRA_EPISODE_NUM = "EXTRA_EPISODE_NUM"
        const val EXTRA_LANG_TYPE = "EXTRA_LANG_TYPE"
    }

}

