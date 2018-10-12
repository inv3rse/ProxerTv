package com.inverse.unofficial.proxertv.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.ui.search.SearchActivity

class MainActivity : FragmentActivity() {

    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainFragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as MainFragment
    }

    override fun onBackPressed() {
        if (!mainFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSearchRequested(): Boolean {
        startActivity(Intent(this, SearchActivity::class.java))
        return true
    }
}
