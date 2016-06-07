package com.example.dennis.proxertv.ui.player

import android.app.Activity
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.VideoView
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.base.App
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class PlayerActivity : Activity() {

    private lateinit var videoView: VideoView
    private lateinit var mediaSession: MediaSession

    private var playbackState = PlaybackState.LOADING
    private var streamUrls = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoView = findViewById(R.id.videoView) as VideoView

        mediaSession = MediaSession(this, "ProxerTv")
        mediaSession.setCallback(object : MediaSession.Callback() {})
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS and MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.isActive = true

        loadStreamUrl()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.suspend()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
//        val playbackOverlayFragment = fragmentManager.findFragmentById(R.id.playback_controls_fragment) as PlayerOverlayFragment
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                setPlaying(false)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                setPlaying(false)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (playbackState == PlaybackState.PLAYING) {
                    setPlaying(false)
                } else {
                    setPlaying(true)
                }
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
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
                    if (playbackState == PlaybackState.LOADING) {
                        setStream(it)
                    }
                }, { it.printStackTrace() }, {})
    }

    private fun setStream(url: String) {
        videoView.setVideoURI(Uri.parse(url))
        videoView.start()

        playbackState = PlaybackState.PLAYING
    }

    private fun setPlaying(playing: Boolean) {
        if (playbackState != PlaybackState.LOADING) {

            if (playing && playbackState != PlaybackState.PLAYING) {
                videoView.resume()
                playbackState = PlaybackState.PLAYING
            } else if (playbackState != PlaybackState.PAUSED) {
                videoView.pause()
                playbackState = PlaybackState.PAUSED
            }
        }
    }

    enum class PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE, LOADING
    }

    companion object {
        const val EXTRA_SERIES_ID = "EXTRA_SERIES_ID"
        const val EXTRA_EPISODE_NUM = "EXTRA_EPISODE_NUM"
        const val EXTRA_LANG_TYPE = "EXTRA_LANG_TYPE"
    }

}

