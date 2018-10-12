package com.inverse.unofficial.proxertv.ui.search

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.inverse.unofficial.proxertv.R

class SearchActivity : FragmentActivity() {

    private lateinit var searchFragment: MySearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchFragment = supportFragmentManager.findFragmentById(R.id.search_fragment) as MySearchFragment
    }

    override fun onSearchRequested(): Boolean {
        searchFragment.startRecognition()
        return true
    }
}
