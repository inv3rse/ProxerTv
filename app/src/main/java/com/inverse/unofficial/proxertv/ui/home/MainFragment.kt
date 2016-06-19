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
import com.inverse.unofficial.proxertv.ui.util.CoverCardPresenter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class MainFragment : BrowseFragment(), OnItemViewClickedListener, View.OnClickListener {
    private val coverPresenter = CoverCardPresenter()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val myListAdapter = ArrayObjectAdapter(coverPresenter)
    private val topAccessAdapter = ArrayObjectAdapter(coverPresenter)
    private val topRatingAdapter = ArrayObjectAdapter(coverPresenter)
    private val airingAdapter = ArrayObjectAdapter(coverPresenter)

    private val handler = Handler()

    private lateinit var backgroundManager: BackgroundManager
    private lateinit var metrics: DisplayMetrics

    private val subscriptions = CompositeSubscription()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

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
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_top_access)), topAccessAdapter))
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_top_rating)), topRatingAdapter))
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_airing)), airingAdapter))

        adapter = rowsAdapter
    }

    private fun loadContent() {
        val client = App.component.getProxerClient()
        val myListRepository = App.component.getMySeriesRepository()

        subscriptions.add(client.loadTopAccessSeries()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { topAccessAdapter.addAll(0, it) },
                        { it.printStackTrace() }, { }
                ))

        subscriptions.add(client.loadTopRatingSeries()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { topRatingAdapter.addAll(0, it) },
                        { it.printStackTrace() }, { }
                ))

        subscriptions.add(client.loadAiringSeries()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { airingAdapter.addAll(0, it) },
                        { it.printStackTrace() }, { }
                ))

        subscriptions.add(myListRepository.observeSeriesList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    myListAdapter.clear()
                    myListAdapter.addAll(0, it)
                }))
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