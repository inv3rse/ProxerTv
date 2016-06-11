package com.example.dennis.proxertv.ui.details

import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.example.dennis.proxertv.R
import com.example.dennis.proxertv.base.App
import com.example.dennis.proxertv.base.ProxerClient
import com.example.dennis.proxertv.model.Episode
import com.example.dennis.proxertv.model.Series
import com.example.dennis.proxertv.ui.player.PlayerActivity
import com.example.dennis.proxertv.ui.util.CoverCardPresenter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class SeriesDetailsFragment : DetailsFragment(), OnItemViewClickedListener, OnActionClickedListener {
    val presenterSelector = ClassPresenterSelector()
    val contentAdapter = ArrayObjectAdapter(presenterSelector)
    val subscriptions = CompositeSubscription()

    lateinit var client: ProxerClient
    var series: Series? = null
    var currentEpisodePage = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = App.component.getProxerClient()

        setupPresenter()
        loadContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
        if (item is Episode) {
            val intent = Intent(activity, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.EXTRA_SERIES_ID, item.seriesId)
            intent.putExtra(PlayerActivity.EXTRA_EPISODE_NUM, item.episodeNum)
            intent.putExtra(PlayerActivity.EXTRA_LANG_TYPE, item.languageType)

            activity.startActivity(intent)
        }
    }

    override fun onActionClicked(action: Action) {
        if (series != null) {
            loadEpisodes(series!!, action.id.toInt())
        }
    }

    @Suppress("DEPRECATION")
    private fun setupPresenter() {
        val detailsOverviewPresenter = DetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsOverviewPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        detailsOverviewPresenter.onActionClickedListener = this
        onItemViewClickedListener = this

        adapter = contentAdapter
    }

    private fun loadContent() {
        val seriesId = activity.intent.extras.getInt(DetailsActivity.EXTRA_SERIES_ID)
        subscriptions.add(client.loadSeries(seriesId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ series ->
                    if (series != null) {
                        this.series = series
                        loadEpisodes(series, 1)

                        val detailsRow = DetailsOverviewRow(series)
                        if (series.pages > 1) {
                            val actionsAdapter = ArrayObjectAdapter()
                            for (i in 1..series.pages) {
                                actionsAdapter.add(Action(i.toLong(), getString(R.string.page_title, i)))
                            }
                            detailsRow.actionsAdapter = actionsAdapter
                        }

                        contentAdapter.add(detailsRow)

                        Glide.with(activity)
                                .load(series.imageUrl)
                                .centerCrop()
                                .into(object : SimpleTarget<GlideDrawable>(280, 392) {
                                    override fun onResourceReady(resource: GlideDrawable, glideAnimation: GlideAnimation<in GlideDrawable>?) {
                                        detailsRow.imageDrawable = resource
                                        contentAdapter.notifyArrayItemRangeChanged(0, contentAdapter.size())
                                    }

                                })
                    }
                }, { it.printStackTrace() }, {}))
    }

    private fun loadEpisodes(series: Series, page: Int) {
        if (currentEpisodePage != page) {
            currentEpisodePage = page
            contentAdapter.removeItems(1, contentAdapter.size() - 1)

            subscriptions.add(client.loadEpisodesPage(series.id, page).subscribe(fun(episodesMap: Map<String, List<Int>>) {
                val cardPresenter = CoverCardPresenter()
                episodesMap.keys.forEach { name ->
                    val header = HeaderItem(name)
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                    val episodes = episodesMap[name] ?: emptyList()
                    for (i in episodes) {
                        listRowAdapter.add(Episode(series.id, i, name, series.imageUrl))
                    }

                    contentAdapter.add(ListRow(header, listRowAdapter))
                }
            }, { it.printStackTrace() }, {}))
        }
    }

    companion object {
        private const val ARG_SERIES_ID = "ARG_SERIES_ID"

        fun createInstance(seriesId: Int): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle()
            args.putInt(ARG_SERIES_ID, seriesId)
            fragment.arguments = args

            return fragment
        }
    }
}