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
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.search.SearchActivity
import com.inverse.unofficial.proxertv.ui.util.SeriesCoverPresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.TimeUnit

class MainFragment : BrowseFragment(), OnItemViewClickedListener, View.OnClickListener {
    private val coverPresenter = SeriesCoverPresenter()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val myListAdapter = ArrayObjectAdapter(coverPresenter)
    private val handler = Handler()
    private lateinit var backgroundManager: BackgroundManager

    // access to ListRow and corresponding adapter based on target position
    private val rowTargetMap = mutableMapOf<ListRow, Int>()
    private val targetRowMap = mutableMapOf<Int, ArrayObjectAdapter>()

    private lateinit var metrics: DisplayMetrics

    private val subscriptions = CompositeSubscription()
    private val progressRepository = App.component.getSeriesProgressRepository()
    private val myListRepository = App.component.getMySeriesRepository()
    private val client = App.component.getProxerClient()

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
        // we need at least one row before onStart or the BrowseFragment crashes,
        // https://code.google.com/p/android/issues/detail?id=214795
        rowsAdapter.add(ListRow(HeaderItem(getString(R.string.row_my_list)), myListAdapter))
        adapter = rowsAdapter
    }

    private fun loadContent() {
        subscriptions.add(myListRepository.observeSeriesList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    myListAdapter.clear()
                    myListAdapter.addAll(0, it)
                }))

        loadAndAddRow(Observable.interval(0, 30, TimeUnit.MINUTES)
                .flatMap { loadEpisodesUpdateRow() }, getString(R.string.row_updates), 1)

        loadAndAddRow(client.loadTopAccessSeries(), getString(R.string.row_top_access), 2)
        loadAndAddRow(client.loadTopRatingSeries(), getString(R.string.row_top_rating), 3)
        loadAndAddRow(client.loadTopRatingMovies(), getString(R.string.row_top_rating_movies), 4)
        loadAndAddRow(client.loadAiringSeries(), getString(R.string.row_airing), 5)
    }

    private fun loadAndAddRow(loadObservable: Observable<List<SeriesCover>>, rowName: String, position: Int) {
        subscriptions.add(loadObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            val existingAdapter = targetRowMap[position]
                            if (existingAdapter != null) {
                                if (!it.isEmpty()) {
                                    // update existing content
                                    existingAdapter.clear()
                                    existingAdapter.addAll(0, it)
                                } else {
                                    // list is empty, remove row
                                    for (i in 1..rowsAdapter.size()) {
                                        val row = rowsAdapter.get(i - 1)
                                        val rowTarget = rowTargetMap[row]
                                        if (position == rowTarget) {
                                            rowsAdapter.remove(row)
                                            rowTargetMap.remove(row)
                                            targetRowMap.remove(position)
                                            break
                                        }
                                    }
                                }
                            } else if (it.size > 0) {
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
                                targetRowMap.put(position, adapter)
                            }
                        }, { it.printStackTrace() }
                ))
    }

    private fun loadEpisodesUpdateRow(): Observable<List<SeriesCover>> {
        val calendar = GregorianCalendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -3)
        val lastUpdateDate = calendar.time

        return client.loadUpdatesList()
                .flatMap { Observable.from(it) }
                .filter { it.updateDate > lastUpdateDate }
                .map { it.seriesCover }
                .distinct()
                .map {
                    Observable.combineLatest(
                            Observable.just(it),
                            progressRepository.observeProgress(it.id),
                            { series, progress -> Pair(series, progress) })
                }
                .toList()
                .flatMap(fun(observables: List<Observable<Pair<SeriesCover, Int>>>): Observable<List<Pair<SeriesCover, Int>>> {
                    return Observable.combineLatest(
                            observables,
                            fun(array: Array<out Any>): List<Pair<SeriesCover, Int>> {
                                val seriesList = arrayListOf<Pair<SeriesCover, Int>>()
                                for (element in array) {
                                    @Suppress("UNCHECKED_CAST")
                                    val seriesProgressPair = element as Pair<SeriesCover, Int>
                                    seriesList.add(seriesProgressPair)
                                }

                                return seriesList
                            }
                    )
                })
                .flatMap {
                    Observable.from(it)
                            .filter { it.second > 0 }
                            .flatMap {
                                Observable.zip(
                                        Observable.just(it),
                                        client.loadEpisodesPage(it.first.id, ProxerClient.getTargetPageForEpisode(it.second + 1)),
                                        { seriesProgress, episodesMap -> Triple(seriesProgress.first, seriesProgress.second, episodesMap) })
                            }
                            .filter { it.third.any { entry -> entry.value.contains(it.second + 1) } }
                            .map { it.first }
                            .toList()
                }
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