package com.inverse.unofficial.proxertv.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.ui.search.SearchActivity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, SearchActivity::class.java))
        return true
    }
}
