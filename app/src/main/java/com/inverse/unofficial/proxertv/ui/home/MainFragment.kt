package com.inverse.unofficial.proxertv.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.annotation.StringRes
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.widget.*
import android.support.v4.app.ActivityOptionsCompat
import android.view.View
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.base.User
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.SeriesList
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.login.LoginActivity
import com.inverse.unofficial.proxertv.ui.search.SearchActivity
import com.inverse.unofficial.proxertv.ui.util.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.*

/**
 * Main screen of the app. Shows multiple rows of series items with only name and cover.
 */
class MainFragment : BrowseFragment(), OnItemViewClickedListener, View.OnClickListener {
    private val presenterSelector = CoverPresenterSelector()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val seriesUpdateHandler = Handler()

    // access to ListRow and corresponding adapter based on target position
    private val rowTargetMap = mutableMapOf<ListRow, Int>()
    private val targetRowMap = mutableMapOf<Int, ObjectAdapter>()

    private val subscriptions = CompositeSubscription()
    private var syncSubscription: Subscription? = null
    private val proxerRepository = App.component.getProxerRepository()
    private val userSettings = App.component.getUserSettings()
    private val userRowAdapter = UserActionAdapter(userSettings.getUser() != null)

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

        onItemViewClickedListener = this
        setOnSearchClickedListener(this)

        initDefaultRows()
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
                    DetailsActivity.SHARED_ELEMENT_COVER).toBundle()

            intent.putExtra(DetailsActivity.EXTRA_SERIES_ID, item.id)
            startActivity(intent, bundle)
        } else if (item is UserActionHolder) {
            when (item.userAction) {
                UserAction.LOGIN -> startActivity<LoginActivity>()
                UserAction.LOGOUT -> proxerRepository.logout().subscribeOn(Schedulers.io()).subscribe()
                UserAction.SYNC -> if (!item.isLoading) {
                    synchronizeAccount()
                }
            }
        }
    }

    override fun onClick(view: View) {
        val intent = Intent(activity, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun initDefaultRows() {
        addRow(R.string.row_account_actions, userRowAdapter, POS_ACCOUNT_ACTIONS_LIST)

        subscriptions.add(userSettings.observeAccount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { user: User? -> userRowAdapter.loggedIn = user != null },
                        { CrashReporting.logException(it) }))

        adapter = rowsAdapter
    }

    private fun loadContent() {
        val userListObservable = proxerRepository.syncUserList()
                .flatMap { proxerRepository.observeSeriesList() }
                .subscribeOn(Schedulers.io())
                .publish()

        loadAndAddRow(userListObservable.map { list -> list.filter { it.userList == SeriesList.WATCHLIST } },
                R.string.row_my_list, POS_USER_LIST)

        loadAndAddRow(updateSubject.flatMap { loadEpisodesUpdateRow().takeUntil(updateSubject) },
                R.string.row_updates, POS_UPDATES_LIST)

        loadAndAddRow(proxerRepository.loadTopAccessSeries(), R.string.row_top_access, POS_TOP_ACCESS_LIST)
        loadAndAddRow(proxerRepository.loadTopRatingSeries(), R.string.row_top_rating, POS_TOP_RATING_LIST)
        loadAndAddRow(proxerRepository.loadTopRatingMovies(), R.string.row_top_rating_movies, POS_TOP_MOVIES_LIST)
        loadAndAddRow(proxerRepository.loadAiringSeries(), R.string.row_airing, POS_AIRING_LIST)

        loadAndAddRow(userListObservable.map { list -> list.filter { it.userList == SeriesList.FINISHED } },
                R.string.row_user_finished, 7)

        loadAndAddRow(userListObservable.map { list -> list.filter { it.userList == SeriesList.ABORTED } },
                R.string.row_user_aborted, 8)

        subscriptions.add(userListObservable.connect())
    }

    /**
     * Adds a row to the existing rows according to targetPos.
     */
    private fun addRow(@StringRes headerName: Int, adapter: ObjectAdapter, targetPos: Int) {
        val listRow = ListRow(HeaderItem(getString(headerName)), adapter)

        var targetIndex = rowsAdapter.size()
        // iterate over the existing rows to find the correct index for our targetPos
        // (the last row might be the only one added yet)
        for (i in 1..rowsAdapter.size()) {
            val rowTarget = rowTargetMap[rowsAdapter.get(i - 1)] ?: 0
            if (targetPos < rowTarget) {
                targetIndex = i - 1
                break
            }
        }

        rowsAdapter.add(targetIndex, listRow)
        rowTargetMap.put(listRow, targetPos)
        targetRowMap.put(targetPos, adapter)
    }

    /**
     * Removes the row for targetPos
     */
    private fun removeRow(targetPos: Int) {
        rowTargetMap.filterValues { it == targetPos }.forEach { entry ->
            rowsAdapter.remove(entry.key)
            rowTargetMap.remove(entry.key)
            targetRowMap.remove(entry.value)
        }
    }

    /**
     * Subscribes to the loadObservable and sets the row content once it emits items. If the onNext List is empty or the
     * observable throws an error the row will be removed.
     */
    private fun loadAndAddRow(loadObservable: Observable<out List<ISeriesCover>>, @StringRes headerName: Int, position: Int, addFirst: Boolean = true) {
        if (addFirst && targetRowMap[position] == null) {
            // add the empty row first, before the content for it is loaded
            // it is necessary to keep the selection on app start at the top of the page
            val adapter = ArrayObjectAdapter(presenterSelector)
            adapter.add(LoadingCover())
            addRow(headerName, adapter, position)
        }

        subscriptions.add(loadObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { seriesList ->
                            val existingAdapter = targetRowMap[position]
                            if (existingAdapter != null && existingAdapter is ArrayObjectAdapter) {
                                if (!seriesList.isEmpty()) {
                                    // update existing content
                                    existingAdapter.clear()
                                    existingAdapter.addAll(0, seriesList)
                                } else {
                                    // list is empty, remove it
                                    removeRow(position)
                                }
                            } else if (seriesList.isNotEmpty()) {
                                val adapter = ArrayObjectAdapter(presenterSelector)
                                adapter.addAll(0, seriesList)
                                addRow(headerName, adapter, position)
                            }
                        },
                        { error ->
                            removeRow(position)
                            CrashReporting.logException(error)
                        }
                ))
    }

    private fun loadEpisodesUpdateRow(): Observable<List<SeriesCover>> {
        val calendar = GregorianCalendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -UPDATES_HISTORY)
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

    private fun synchronizeAccount() {
        syncSubscription?.unsubscribe()
        userRowAdapter.addLoading(UserAction.SYNC)
        syncSubscription = proxerRepository.syncUserList(true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            userRowAdapter.removeLoading(UserAction.SYNC)
                            syncSubscription = null
                        },
                        {
                            userRowAdapter.removeLoading(UserAction.SYNC)
                            syncSubscription = null
                            toast(R.string.user_sync_failed)
                            CrashReporting.logException(it)
                        })
    }

    companion object {
        private const val SERIES_UPDATE_DELAY: Long = 30 * 60 * 1000 // 30 minutes in milliseconds
        private const val UPDATES_HISTORY = 3 // days

        private const val POS_USER_LIST = 0
        private const val POS_UPDATES_LIST = 1
        private const val POS_TOP_ACCESS_LIST = 2
        private const val POS_TOP_RATING_LIST = 3
        private const val POS_TOP_MOVIES_LIST = 4
        private const val POS_AIRING_LIST = 5
        private const val POS_ACCOUNT_ACTIONS_LIST = 6

    }
}