package com.inverse.unofficial.proxertv.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v17.leanback.app.BackgroundManager
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import android.util.DisplayMetrics
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.search.SearchActivity
import com.inverse.unofficial.proxertv.ui.util.SeriesCoverPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MainFragment : BrowseFragment(), OnItemViewClickedListener, View.OnClickListener {
    private val coverPresenter = SeriesCoverPresenter()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val myListAdapter = ArrayObjectAdapter(coverPresenter)
    private val rowTargetMap = mutableMapOf<ListRow, Int>()
    private val handler = Handler()

    private lateinit var backgroundManager: BackgroundManager
    private lateinit var metrics: DisplayMetrics

    private val subscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_HIDDEN

        initEmptyRows()
        onItemViewClickedListener = this
        setOnSearchClickedListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        backgroundManager = BackgroundManager.getInstance(activity)
        backgroundManager.attach(activity.window)

        metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)

        loadContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
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

    override fun onClick(view: View) {
        val intent = Intent(activity, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun initEmptyRows() {
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_my_list)), myListAdapter))
        adapter = rowsAdapter
    }

    private fun loadContent() {
        val client = App.component.getProxerClient()
        val myListRepository = App.component.getMySeriesRepository()

        subscriptions.add(myListRepository.observeSeriesList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    myListAdapter.clear()
                    myListAdapter.addAll(0, it)
                }))

        loadAndAddRow(client.loadTopAccessSeries(), getString(R.string.row_top_access), 1)
        loadAndAddRow(client.loadTopRatingSeries(), getString(R.string.row_top_rating), 2)
        loadAndAddRow(client.loadTopRatingMovies(), getString(R.string.row_top_rating_movies), 3)
        loadAndAddRow(client.loadAiringSeries(), getString(R.string.row_airing), 4)
    }

    private fun loadAndAddRow(loadObservable: Observable<List<SeriesCover>>, rowName: String, position: Int) {
        subscriptions.add(loadObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            if (it.size > 0) {
                                val adapter = ArrayObjectAdapter(coverPresenter)
                                adapter.addAll(0, it)
                                val listRow = ListRow(HeaderItem(rowName), adapter)
                                var targetIndex = rowsAdapter.size()
                                for (i in 1..rowsAdapter.size()) {
                                    val rowTarget = rowTargetMap[rowsAdapter.get(i - 1)] ?: 0
                                    if (position < rowTarget) {
                                        targetIndex = i - 1
                                        break
                                    }
                                }
                                rowsAdapter.add(targetIndex, listRow)
                                rowTargetMap.put(listRow, position)
                            }
                        }, { it.printStackTrace() }
                ))
    }

    private fun setBackgroundImage(uri: String) {
        Glide.with(activity)
                .load(uri)
                .centerCrop()
                .into(object : SimpleTarget<GlideDrawable>(metrics.widthPixels, metrics.heightPixels) {
                    override fun onResourceReady(resource: GlideDrawable, glideAnimation: GlideAnimation<in GlideDrawable>) {
                        backgroundManager.drawable = resource
                    }
                })
    }

    private fun scheduleBackgroundUpdate(uri: String) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ setBackgroundImage(uri) }, BACKGROUND_UPDATE_DELAY)
    }

    companion object {
        const val BACKGROUND_UPDATE_DELAY: Long = 300
    }

}