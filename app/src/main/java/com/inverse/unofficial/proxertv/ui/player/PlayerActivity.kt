package com.inverse.unofficial.proxertv.ui.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.os.BuildCompat
import com.inverse.unofficial.proxertv.R

class PlayerActivity : Activity() {
    private lateinit var overlayFragment: PlayerOverlayFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        overlayFragment = fragmentManager.findFragmentById(R.id.playback_controls_fragment) as PlayerOverlayFragment
    }

    override fun onVisibleBehindCanceled() {
        mediaController.transportControls.pause()
        super.onVisibleBehindCanceled()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        overlayFragment.initEpisode()
    }

    companion object {
        const val EXTRA_EPISODE = "EXTRA_EPISODE"
        const val EXTRA_SERIES = "EXTRA_SERIES"

        fun supportsPictureInPicture(context: Context): Boolean {
            return BuildCompat.isAtLeastN() && context.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }
    }
}

