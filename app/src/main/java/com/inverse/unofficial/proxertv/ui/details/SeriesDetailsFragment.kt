package com.inverse.unofficial.proxertv.ui.details

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.widget.*
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.ui.player.PlayerActivity
import com.inverse.unofficial.proxertv.ui.util.EpisodeAdapter
import com.inverse.unofficial.proxertv.ui.util.EpisodePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

/**
 * The details view for a series.
 */
class SeriesDetailsFragment : DetailsFragment(), OnItemViewClickedListener, SeriesDetailsRowPresenter.SeriesDetailsRowListener {
    private val presenterSelector = ClassPresenterSelector()
    private val contentAdapter = ArrayObjectAdapter(presenterSelector)
    private val subscriptions = CompositeSubscription()

    private val proxerRepository = App.component.getProxerRepository()

    private var episodeSubscription: Subscription? = null
    private var series: Series? = null
    private var currentPage = 0

    private val episodeAdapters = arrayListOf<EpisodeAdapter>()
    private val detailsOverviewPresenter = SeriesDetailsRowPresenter(this)
    private lateinit var seriesProgress: Observable<Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity.postponeEnterTransition()
        val handler = Handler()

        handler.postDelayed({ activity.startPostponedEnterTransition() }, MAX_TRANSITION_DELAY)
        detailsOverviewPresenter.coverReadyListener = {
            activity.startPostponedEnterTransition()
            handler.removeCallbacksAndMessages(null)
        }

        setupPresenter()
        loadContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
        episodeSubscription?.unsubscribe()
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is EpisodeAdapter.EpisodeHolder) {
            val intent = Intent(activity, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.EXTRA_EPISODE, item.episode)
            intent.putExtra(PlayerActivity.EXTRA_SERIES, series)
            activity.startActivity(intent)
        }
    }

    override fun onSelectListClicked(seriesRow: SeriesDetailsRowPresenter.SeriesDetailsRow) {
        series?.let {
            fragmentManager.beginTransaction()
                    .add(android.R.id.content, SideMenuFragment.create(it))
                    .addToBackStack(null)
                    .commit()
        }
    }

    override fun onPageSelected(seriesRow: SeriesDetailsRowPresenter.SeriesDetailsRow, selection: PageSelection) {
        // load selected episode page
        if ((selection.pageNumber - 1) != currentPage) {
            loadEpisodes(seriesRow.series, selection.pageNumber - 1)
            seriesRow.currentPageNumber = selection.pageNumber
        }
    }

    private fun setupPresenter() {
        presenterSelector.addClassPresenter(SeriesDetailsRowPresenter.SeriesDetailsRow::class.java, detailsOverviewPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        onItemViewClickedListener = this
        adapter = contentAdapter
    }

    private fun loadContent() {
        val seriesId = activity.intent.extras.getInt(DetailsActivity.EXTRA_SERIES_ID)

        seriesProgress = proxerRepository.observeSeriesProgress(seriesId).replay(1).refCount()

        // update episode progress
        subscriptions.add(seriesProgress
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    for (adapter in episodeAdapters) {
                        adapter.progress = it
                    }
                }, { it.printStackTrace() }))

        // update series list state
        subscriptions.add(proxerRepository.observerSeriesListState(seriesId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ seriesList ->
                    detailsOverviewPresenter.seriesList = seriesList
                    adapter.notifyItemRangeChanged(0, 1)
                }))

        val observable = Observable.zip(
                proxerRepository.loadSeries(seriesId),
                seriesProgress.first(),
                { series, progress -> Pair(series, progress) })

        subscriptions.add(
                observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ (series, progress) ->
                            val nextEpisode = progress + 1

                            if (series != null) {
                                this.series = series
                                val episodesPage = ProxerClient.getTargetPageForEpisode(nextEpisode)
                                loadEpisodes(series, episodesPage)

                                val detailsRow = SeriesDetailsRowPresenter.SeriesDetailsRow(series, episodesPage + 1)
                                contentAdapter.add(detailsRow)
                            }
                        }, { CrashReporting.logException(it) }))
    }

    private fun loadEpisodes(series: Series, page: Int) {
        // remove old episode list
        contentAdapter.removeItems(1, contentAdapter.size() - 1)
        episodeSubscription?.unsubscribe()

        episodeSubscription = Observable.zip(
                proxerRepository.loadEpisodesPage(series.id, page),
                seriesProgress.first(),
                { episodes, progress -> Pair(episodes, progress) })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fun(episodesProgress: Pair<Map<String, List<Int>>, Int>) {
                    // remove old episodes (if the action is not triggered by calling the function)
                    contentAdapter.removeItems(1, contentAdapter.size() - 1)
                    episodeAdapters.clear()

                    val (episodesMap, progress) = episodesProgress
                    val episodePresenter = EpisodePresenter(series.id)

                    for (subType in episodesMap.keys) {
                        val header = HeaderItem(subType)

                        val episodes = episodesMap[subType]?.map { Episode(it, subType) } ?: emptyList()
                        val adapter = EpisodeAdapter(episodes, progress, episodePresenter)

                        episodeAdapters.add(adapter)
                        contentAdapter.add(ListRow(header, adapter))
                    }
                    currentPage = page

                }, { CrashReporting.logException(it) })
    }

    companion object {
        private const val MAX_TRANSITION_DELAY = 4000L
    }
}