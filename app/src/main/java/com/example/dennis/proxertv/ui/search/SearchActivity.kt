package com.example.dennis.proxertv.ui.search

import android.app.Activity
import android.os.Bundle
import com.example.dennis.proxertv.R

class SearchActivity : Activity() {

    lateinit var searchFragment: MySearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchFragment = fragmentManager.findFragmentById(R.id.search_fragment) as MySearchFragment
    }

    override fun onSearchRequested(): Boolean {
        searchFragment.startRecognition()
        return true
    }
}
