package com.inverse.unofficial.proxertv.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.User
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.SeriesList
import com.inverse.unofficial.proxertv.ui.details.DetailsActivity
import com.inverse.unofficial.proxertv.ui.home.login.LoginActivity
import com.inverse.unofficial.proxertv.ui.home.logout.LogoutActivity
import com.inverse.unofficial.proxertv.ui.search.SearchActivity
import com.inverse.unofficial.proxertv.ui.util.*
import org.jetbrains.anko.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.*

/**
 * Main screen of the app. Shows multiple rows of series items with only name and cover.
 */
class MainFragment : BrowseSupportFragment(), OnItemViewClickedListener, View.OnClickListener,
    OnItemViewSelectedListener {
    private val seriesUpdateHandler = Handler()
    private val subscriptions = CompositeSubscription()
    private var syncSubscription: Subscription? = null

    private val proxerRepository = App.component.getProxerRepository()
    private val userSettings = App.component.getUserSettings()
    private val userRowAdapter = UserActionAdapter(userSettings.getUser() != null)

    private lateinit var rowsHelper: RowsHelper
    private var currentRowItemIndex: Int = 0

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

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        rowsHelper = RowsHelper(rowsAdapter, resources)
        adapter = rowsAdapter

        onItemViewClickedListener = this
        onItemViewSelectedListener = this
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
        rowsHelper.cancelPendingOperations()
    }

    override fun onItemSelected(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (row is ListRow) {
            val adapter = row.adapter
            when (adapter) {
                is ArrayObjectAdapter -> {
                    currentRowItemIndex = adapter.indexOf(item)
                    rowsHelper.onItemSelected(currentRowItemIndex, adapter, row)
                }
                is UserActionAdapter -> {
                    // different method, does not implement the collections interface
                    currentRowItemIndex = adapter.indexOf(item)
                }
            }

        }
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (item is ISeriesCover) {
            val intent = Intent(activity, DetailsActivity::class.java)
            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                (itemViewHolder.view as ImageCardView).mainImageView,
                DetailsActivity.SHARED_ELEMENT_COVER
            ).toBundle()

            intent.putExtra(DetailsActivity.EXTRA_SERIES_ID, item.id)
            startActivity(intent, bundle)
        } else if (item is UserActionHolder) {
            when (item.userAction) {
                UserAction.LOGIN -> startActivity(LoginActivity.createIntent(requireContext()))
                UserAction.LOGOUT -> startActivity(LogoutActivity.createIntent(requireContext()))
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

    /**
     * React to a back pressed event
     * @return true if we handled the event
     */
    fun onBackPressed(): Boolean {
        if (currentRowItemIndex > 0) {
            val selectTask = ListRowPresenter.SelectItemViewHolderTask(0)
            selectTask.isSmoothScroll = currentRowItemIndex < SMOOTH_SCROLL_LIMIT
            setSelectedPosition(selectedPosition, false, selectTask)
            return true
        }
        return false
    }

    private fun initDefaultRows() {
        rowsHelper.addRow(userRowAdapter, R.string.row_account_actions, POS_ACCOUNT_ACTIONS_LIST)

        subscriptions.add(
            userSettings.observeAccount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { user: User? -> userRowAdapter.loggedIn = user != null },
                    { Timber.e(it) })
        )

    }

    private fun loadContent() {
        val presenterSelector = CoverPresenterSelector(GlideApp.with(this))
        val userListObservable = proxerRepository.syncUserList()
            .flatMap { proxerRepository.observeSeriesList() }
            .subscribeOn(Schedulers.io())
            .publish()

        rowsHelper.addObservableRow(
            ArrayObjectAdapter(presenterSelector),
            userListObservable.map { list -> list.filter { it.userList == SeriesList.WATCHLIST } },
            R.string.row_my_list, POS_USER_LIST
        )

        rowsHelper.addObservableRow(
            ArrayObjectAdapter(presenterSelector),
            updateSubject.flatMap { getUpdatesRowObservable().takeUntil(updateSubject) },
            R.string.row_updates, POS_UPDATES_LIST
        )

        rowsHelper.addObservablePagingRow(
            ArrayObjectAdapter(presenterSelector),
            { proxerRepository.loadTopAccessSeries(it) }, R.string.row_top_access, POS_TOP_ACCESS_LIST
        )

        rowsHelper.addObservablePagingRow(
            ArrayObjectAdapter(presenterSelector),
            { proxerRepository.loadTopRatingSeries(it) }, R.string.row_top_rating, POS_TOP_RATING_LIST
        )

        rowsHelper.addObservablePagingRow(
            ArrayObjectAdapter(presenterSelector),
            { proxerRepository.loadTopRatingMovies(it) }, R.string.row_top_rating_movies, POS_TOP_MOVIES_LIST
        )

        rowsHelper.addObservablePagingRow(
            ArrayObjectAdapter(presenterSelector),
            { proxerRepository.loadAiringSeries(it) }, R.string.row_airing, POS_AIRING_LIST
        )

        rowsHelper.addObservableRow(
            ArrayObjectAdapter(presenterSelector),
            userListObservable.map { list -> list.filter { it.userList == SeriesList.FINISHED } },
            R.string.row_user_finished, POS_FINISHED_LIST
        )

        rowsHelper.addObservableRow(
            ArrayObjectAdapter(presenterSelector),
            userListObservable.map { list -> list.filter { it.userList == SeriesList.ABORTED } },
            R.string.row_user_aborted, POS_ABORTED_LIST
        )

        subscriptions.add(userListObservable.connect())
    }


    private fun getUpdatesRowObservable(): Observable<List<SeriesCover>> {
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
                    proxerRepository.observeSeriesList().map { myList -> myList.find { s -> s.id == it.id } != null }
                ) { series, progress, inList -> Triple(series, progress, inList) }
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
            .flatMap { list ->
                Observable.from(list)
                    .filter { it.second > 0 && it.third }
                    .flatMap {
                        Observable.zip(
                            Observable.just(it),
                            proxerRepository.loadEpisodesPage(
                                it.first.id,
                                ProxerClient.getTargetPageForEpisode(it.second + 1)
                            )
                        ) { (seriesCover, progress), episodesMap -> Triple(seriesCover, progress, episodesMap) }
                    }
                    // check if any episode subtype contains the next episode according to the users progress
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
                    requireContext().toast(R.string.user_sync_failed)
                    Timber.e(it)
                })
    }

    companion object {
        private const val SERIES_UPDATE_DELAY: Long = 30 * 60 * 1000 // 30 minutes in milliseconds
        private const val UPDATES_HISTORY = 3 // days
        private const val SMOOTH_SCROLL_LIMIT = 40

        private const val POS_USER_LIST = 0
        private const val POS_UPDATES_LIST = 1
        private const val POS_TOP_ACCESS_LIST = 2
        private const val POS_TOP_RATING_LIST = 3
        private const val POS_TOP_MOVIES_LIST = 4
        private const val POS_AIRING_LIST = 5
        private const val POS_ACCOUNT_ACTIONS_LIST = 6
        private const val POS_FINISHED_LIST = 7
        private const val POS_ABORTED_LIST = 8
    }
}