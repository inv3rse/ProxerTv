package com.example.dennis.proxertv.ui.details

import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.DetailsFragment
import android.support.v17.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.example.dennis.proxertv.base.App
import com.example.dennis.proxertv.base.ProxerClient
import com.example.dennis.proxertv.model.Episode
import com.example.dennis.proxertv.ui.player.PlayerActivity
import com.example.dennis.proxertv.ui.details.DetailsActivity
import com.example.dennis.proxertv.ui.details.DetailsDescriptionPresenter
import com.example.dennis.proxertv.ui.util.CoverCardPresenter
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class SeriesDetailsFragment : DetailsFragment(), OnItemViewClickedListener {
    val presenterSelector = ClassPresenterSelector()
    val contentAdapter = ArrayObjectAdapter(presenterSelector)

    lateinit var client: ProxerClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = App.component.getProxerClient()

        setupPresenter()
        loadContent()
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

    private fun setupPresenter() {
        val detailsOverviewPresenter = DetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        presenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsOverviewPresenter)
        presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())

        onItemViewClickedListener = this

        adapter = contentAdapter
    }

    private fun loadContent() {
        val id = activity.intent.extras.getInt(DetailsActivity.EXTRA_SERIES_ID)
        client.loadSeries(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ series ->
                    if (series != null) {
                        val detailsRow = DetailsOverviewRow(series)
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
                        val cardPresenter = CoverCardPresenter()
                        series.availAbleEpisodes.keys.forEach { name ->
                            val header = HeaderItem(name)
                            val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                            val episodes = series.availAbleEpisodes[name] ?: emptyList()
                            for (i in episodes) {
                                listRowAdapter.add(Episode(series.id, i, name, series.imageUrl))
                            }

                            contentAdapter.add(ListRow(header, listRowAdapter))
                        }
                    }
                })
    }

    companion object {
        const val ARG_SERIES_ID = "ARG_SERIES_ID"

        fun createInstance(seriesId: Int): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle()
            args.putInt(ARG_SERIES_ID, seriesId)
            fragment.arguments = args

            return fragment
        }
    }
}