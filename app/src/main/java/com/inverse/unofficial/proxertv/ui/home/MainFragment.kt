package com.inverse.unofficial.proxertv.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.login.LoginActivity
import com.inverse.unofficial.proxertv.ui.search.SearchActivity
import com.inverse.unofficial.proxertv.ui.util.SeriesCoverPresenter
import com.inverse.unofficial.proxertv.ui.util.UserAction
import com.inverse.unofficial.proxertv.ui.util.UserActionAdapter
import org.jetbrains.anko.startActivity
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Main screen of the app. Shows multiple rows of series items with only name and cover.
 */
class MainFragment : BrowseFragment(), OnItemViewClickedListener, View.OnClickListener {
    private val coverPresenter = SeriesCoverPresenter()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val seriesUpdateHandler = Handler()

    // access to ListRow and corresponding adapter based on target position
    private val rowTargetMap = mutableMapOf<ListRow, Int>()
    private val targetRowMap = mutableMapOf<Int, ObjectAdapter>()

    private val subscriptions = CompositeSubscription()
    private val proxerRepository = App.component.getProxerRepository()
    private val userSettings = App.component.getUserSettings()

    private val updateSubject = PublishSubject.create<Int>()
    private var nextUpdate: Long? = null
    private val updateRunner = object : Runnable {
        override fun run() {
            updateSubject.onNext(0)
            nextUpdate = Date().time + SERIES_UPDATE_DELAY
            seriesUpdateHandler.postDelayed(this, SERIES_UPDATE_DELAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_HIDDEN

        initDefaultRows()
        onItemViewClickedListener = this
        setOnSearchClickedListener(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        loadContent()
    }

    override fun onStart() {
        super.onStart()
        // little optimization to avoid updating on start if not necessary
        seriesUpdateHandler.removeCallbacks(updateRunner)
        val delay = (nextUpdate ?: 0) - Date().time
        seriesUpdateHandler.postDelayed(updateRunner, Math.max(delay, 0))
    }

    override fun onStop() {
        super.onStop()
        seriesUpdateHandler.removeCallbacks(updateRunner)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is ISeriesCover) {
            val intent = Intent(activity, DetailsActivity::class.java)
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT).toBundle()

            intent.putExtra(DetailsActivity.EXTRA_SERIES_ID, item.id)
            startActivity(intent, bundle)
        } else if (item is UserAction) {
            when (item) {
                UserAction.LOGIN -> startActivity<LoginActivity>()
                UserAction.LOGOUT -> proxerRepository.logout().subscribe()
                UserAction.SYNC -> proxerRepository.syncUserList(true).subscribe()
            }
        }
    }

    override fun onClick(view: View) {
        val intent = Intent(activity, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun initDefaultRows() {
        val userRowAdapter = UserActionAdapter(userSettings)
        val userListRow = ListRow(HeaderItem(getString(R.string.user_action_row)), userRowAdapter)

        rowsAdapter.add(userListRow)
        rowTargetMap.put(userListRow, 6)
        targetRowMap.put(6, userRowAdapter)
        adapter = rowsAdapter
    }

    private fun loadContent() {
        loadAndAddRow(proxerRepository.syncUserList().flatMap { proxerRepository.observeSeriesList() },
                getString(R.string.row_my_list), 0)

        loadAndAddRow(updateSubject.flatMap { loadEpisodesUpdateRow().takeUntil(updateSubject) },
                getString(R.string.row_updates), 1)

        loadAndAddRow(proxerRepository.loadTopAccessSeries(), getString(R.string.row_top_access), 2)
        loadAndAddRow(proxerRepository.loadTopRatingSeries(), getString(R.string.row_top_rating), 3)
        loadAndAddRow(proxerRepository.loadTopRatingMovies(), getString(R.string.row_top_rating_movies), 4)
        loadAndAddRow(proxerRepository.loadAiringSeries(), getString(R.string.row_airing), 5)

    }

    private fun loadAndAddRow(loadObservable: Observable<out List<ISeriesCover>>, rowName: String, position: Int) {
        subscriptions.add(loadObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            val existingAdapter = targetRowMap[position]
                            if (existingAdapter != null && existingAdapter is ArrayObjectAdapter) {
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
                            } else if (it.isNotEmpty()) {
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
                        }, { CrashReporting.logException(it) }
                ))
    }

    private fun loadEpisodesUpdateRow(): Observable<List<SeriesCover>> {
        val calendar = GregorianCalendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -3)
        val lastUpdateDate = calendar.time

        return proxerRepository.loadUpdatesList()
                // series list to single items
                .flatMap { Observable.from(it) }
                // discard anything older than 3 days
                .filter { it.updateDate >= lastUpdateDate }
                // remove the update date information
                .map { it.seriesCover }
                // no duplicates
                .distinct()
                // map to an observable (not subscribed) that combines the series with the local progress and a boolean
                // indicating whether it is on the user list or not
                .map {
                    Observable.combineLatest(
                            Observable.just(it),
                            proxerRepository.observeSeriesProgress(it.id),
                            proxerRepository.observeSeriesList().map { myList -> myList.find { s -> s.id == it.id } != null },
                            { series, progress, inList -> Triple(series, progress, inList) })
                }
                .toList()
                // to automatically re emitting list of series with progress
                // as soon as the progress for a series changes, the list is updated
                .flatMap(fun(observables: List<Observable<Triple<SeriesCover, Int, Boolean>>>): Observable<List<Triple<SeriesCover, Int, Boolean>>> {
                    return Observable.combineLatest(
                            observables,
                            fun(array: Array<out Any>): List<Triple<SeriesCover, Int, Boolean>> {
                                return array.map {
                                    @Suppress("UNCHECKED_CAST")
                                    it as Triple<SeriesCover, Int, Boolean>
                                }
                            }
                    )
                })
                // for series on "my list" where the local progress is > 0 check for a new episode
                .flatMap {
                    Observable.from(it)
                            .filter { it.second > 0 && it.third }
                            .flatMap {
                                Observable.zip(
                                        Observable.just(it),
                                        proxerRepository.loadEpisodesPage(it.first.id, ProxerClient.getTargetPageForEpisode(it.second + 1)),
                                        { seriesProgress, episodesMap -> Triple(seriesProgress.first, seriesProgress.second, episodesMap) })
                            }
                            .filter { it.third.any { entry -> entry.value.contains(it.second + 1) } }
                            .map { it.first }
                            .toList()
                }
    }

    companion object {
        private const val SERIES_UPDATE_DELAY: Long = 30 * 60 * 1000 // 30 minutes in milliseconds
    }
}