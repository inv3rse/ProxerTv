package com.example.dennis.proxertv.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.ui.search.SearchActivity

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
