package com.example.dennis.proxertv.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.dennis.proxertv.R

class SearchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, SearchActivity::class.java))
        return true
    }
}
