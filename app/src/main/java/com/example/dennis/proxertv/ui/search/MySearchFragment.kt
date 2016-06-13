package com.example.dennis.proxertv.ui.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.SearchFragment
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.base.App
import com.example.dennis.proxertv.model.SeriesCover
import com.example.dennis.proxertv.ui.details.DetailsActivity
import com.example.dennis.proxertv.ui.util.CoverCardPresenter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MySearchFragment : SearchFragment(), SearchFragment.SearchResultProvider, OnItemViewClickedListener {
    private val client = App.component.getProxerClient()
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var resultsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupResultAdapter()
        setOnItemViewClickedListener(this)
        setSearchResultProvider(this)
        setSpeechRecognitionCallback { startActivityForResult(recognizerIntent, RECOGNIZE_SPEECH) }
    }

    private fun setupResultAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        resultsAdapter = ArrayObjectAdapter(CoverCardPresenter())

        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_search_results)), resultsAdapter))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RECOGNIZE_SPEECH && resultCode == Activity.RESULT_OK) {
            setSearchQuery(data, true)
        }
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        resultsAdapter.clear()
        client.searchSeries(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ resultList ->
                    resultsAdapter.addAll(0, resultList)
                }, { it.printStackTrace() }, {})
        return true
    }

    override fun getResultsAdapter(): ObjectAdapter? {
        return rowsAdapter
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is SeriesCover) {
            val intent = Intent(activity, DetailsActivity::class.java)
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT).toBundle()

            intent.putExtra(DetailsActivity.EXTRA_SERIES_ID, item.id)
            startActivity(intent, bundle)
        }
    }

    companion object {
        private const val RECOGNIZE_SPEECH = 0
    }
}