package com.inverse.unofficial.proxertv.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.os.BuildCompat
import com.inverse.unofficial.proxertv.R

class PlayerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }

    override fun onVisibleBehindCanceled() {
        mediaController.transportControls.pause()
        super.onVisibleBehindCanceled()
    }

    companion object {
        const val EXTRA_EPISODE = "EXTRA_EPISODE"

        fun supportsPictureInPicture(context: Context): Boolean {
            return BuildCompat.isAtLeastN() && context.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }
    }
}

