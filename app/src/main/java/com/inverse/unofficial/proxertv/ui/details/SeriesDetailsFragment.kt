package com.inverse.unofficial.proxertv.ui.details

import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.model.ServerConfig
import com.inverse.unofficial.proxertv.ui.player.PlayerActivity
import com.inverse.unofficial.proxertv.ui.util.EpisodeAdapter
import com.inverse.unofficial.proxertv.ui.util.EpisodePresenter
import org.jetbrains.anko.toast
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class SeriesDetailsFragment : DetailsFragment(), OnItemViewClickedListener, OnActionClickedListener {
    private val presenterSelector = ClassPresenterSelector()
    private val contentAdapter = ArrayObjectAdapter(presenterSelector)
    private val actionsAdapter = ArrayObjectAdapter()
    private val subscriptions = CompositeSubscription()

    private val proxerRepository = App.component.getProxerRepository()

    private var episodeSubscription: Subscription? = null
    private var series: Series? = null
    private var inList = false
    private var currentPage = 0

    private val episodeAdapters = arrayListOf<EpisodeAdapter>()
    private lateinit var seriesProgress: Observable<Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupPresenter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
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

    override fun onActionClicked(action: Action) {
        if (series != null) {
            if (action.id == ACTION_ADD_REMOVE) {
                // add or remove
                val cover = SeriesCover(series!!.id, series!!.name)
                if (inList) {
                    proxerRepository.removeSeriesFromList(cover.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ },
                                    {
                                        toast("Failed to remove series")
                                        it.printStackTrace()
                                    })
                } else {
                    proxerRepository.addSeriesToList(cover)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ },
                                    {
                                        toast("Failed to add series")
                                        it.printStackTrace()
                                    })
                }

                inList = !inList
                // update action name
                action.label1 = getString(if (inList) R.string.remove_from_list else R.string.add_to_list)
                actionsAdapter.notifyArrayItemRangeChanged(0, 1)
            } else {
                // load selected episode page
                val page = action.id.toInt()
                if (page != currentPage) {
                    loadEpisodes(series!!, page)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupPresenter() {
        val detailsOverviewPresenter = DetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsOverviewPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        detailsOverviewPresenter.setSharedElementEnterTransition(activity, DetailsActivity.SHARED_ELEMENT)
        detailsOverviewPresenter.onActionClickedListener = this
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


        val observable = Observable.zip(
                proxerRepository.loadSeries(seriesId),
                proxerRepository.hasSeriesOnList(seriesId),
                seriesProgress.first(),
                { series, inList, progress -> Triple(series, inList, progress) })

        subscriptions.add(
                observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ triple: Triple<Series?, Boolean, Int> ->
                            val series = triple.first
                            val nextEpisode = triple.third + 1
                            inList = triple.second

                            if (series != null) {
                                this.series = series
                                loadEpisodes(series, ProxerClient.getTargetPageForEpisode(nextEpisode))

                                val detailsRow = DetailsOverviewRow(series)
                                val addRemove = getString(if (inList) R.string.remove_from_list else R.string.add_to_list)
                                actionsAdapter.add(Action(ACTION_ADD_REMOVE, addRemove))

                                if (series.pages() > 1) {
                                    for (i in 1..series.pages()) {
                                        actionsAdapter.add(Action((i - 1).toLong(), getString(R.string.page_title, i)))
                                    }
                                }

                                detailsRow.actionsAdapter = actionsAdapter
                                contentAdapter.add(detailsRow)

                                Glide.with(activity)
                                        .load(ServerConfig.coverUrl(seriesId))
                                        .centerCrop()
                                        .into(object : SimpleTarget<GlideDrawable>(280, 392) {
                                            override fun onResourceReady(resource: GlideDrawable, glideAnimation: GlideAnimation<in GlideDrawable>?) {
                                                detailsRow.imageDrawable = resource
                                                contentAdapter.notifyArrayItemRangeChanged(0, contentAdapter.size())
                                            }

                                        })
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
                        val adapter = EpisodeAdapter(progress, episodePresenter)

                        val episodes = episodesMap[subType] ?: emptyList()
                        for (i in episodes) {
                            adapter.add(Episode(i, subType))
                        }

                        episodeAdapters.add(adapter)
                        contentAdapter.add(ListRow(header, adapter))
                        currentPage = page
                    }

                }, { CrashReporting.logException(it) })
    }

    companion object {
        private const val ACTION_ADD_REMOVE = -1L
    }
}