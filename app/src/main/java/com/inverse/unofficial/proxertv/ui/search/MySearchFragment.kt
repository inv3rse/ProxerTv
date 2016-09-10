package com.inverse.unofficial.proxertv.ui.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.SearchFragment
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.util.SeriesCoverPresenter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MySearchFragment : SearchFragment(), SearchFragment.SearchResultProvider, OnItemViewClickedListener {
    private val client = App.component.getProxerClient()
    private val subscriptions = CompositeSubscription()
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var resultsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupResultAdapter()
        setOnItemViewClickedListener(this)
        setSearchResultProvider(this)
        setSpeechRecognitionCallback {
            try {
                startActivityForResult(recognizerIntent, RECOGNIZE_SPEECH)
            } catch (e: ActivityNotFoundException) {
                // speech recognition is not supported by the device

            }
        }
    }

    private fun setupResultAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        resultsAdapter = ArrayObjectAdapter(SeriesCoverPresenter())

        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_search_results)), resultsAdapter))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RECOGNIZE_SPEECH && resultCode == Activity.RESULT_OK) {
            setSearchQuery(data, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        resultsAdapter.clear()
        subscriptions.clear()

        if (query.isNotBlank()) {
            subscriptions.add(client.searchSeries(query)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ resultList ->
                        resultsAdapter.addAll(0, resultList)
                    }, { CrashReporting.logException(it) }))
        }
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