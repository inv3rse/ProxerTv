package com.inverse.unofficial.proxertv.ui.details

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.ui.player.PlayerActivity
import com.inverse.unofficial.proxertv.ui.util.EpisodeAdapter
import com.inverse.unofficial.proxertv.ui.util.EpisodePresenter
import com.inverse.unofficial.proxertv.ui.util.GlideApp
import com.inverse.unofficial.proxertv.ui.util.SuccessState
import com.inverse.unofficial.proxertv.ui.util.extensions.provideViewModel

/**
 * The details cardView for a series.
 */
class SeriesDetailsFragment : DetailsSupportFragment(), OnItemViewClickedListener,
    SeriesDetailsRowPresenter.SeriesDetailsRowListener {

    private lateinit var detailsAdapter: DetailsAdapter
    private lateinit var episodePresenter: EpisodePresenter

    private lateinit var model: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        detailsOverviewPresenter = SeriesDetailsRowPresenter(GlideApp.with(this), this)

        val activity = requireActivity()
//        activity.postponeEnterTransition()
//
//        val handler = Handler()
//        handler.postDelayed({ activity.startPostponedEnterTransition() }, MAX_TRANSITION_DELAY)
//        detailsOverviewPresenter.coverReadyListener = {
//            activity.startPostponedEnterTransition()
//            handler.removeCallbacksAndMessages(null)
//        }

//        setupPresenter()
//        loadContent()

        val seriesId = activity.intent?.extras?.getInt(DetailsActivity.EXTRA_SERIES_ID)
            ?: throw IllegalArgumentException("seriesId must be set")

        val glide = GlideApp.with(this)
        detailsAdapter = DetailsAdapter(glide, this)
        adapter = detailsAdapter
        episodePresenter = EpisodePresenter(glide, seriesId)
        onItemViewClickedListener = this

        model = provideViewModel(this) { App.component.getDetailsViewModel() }
        model.init(seriesId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.detailState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is SuccessState -> {
                    detailsAdapter.seriesDetails = it.data
                }
            }
        })

        model.episodesState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is SuccessState -> {
                    detailsAdapter.episodes = it.data.map { category ->
                        val header = HeaderItem(category.title)

                        val episodes = category.episodes.map { num -> Episode(num, category.title) }
                        val adapter = EpisodeAdapter(episodes, category.progress, episodePresenter)

                        ListRow(header, adapter)
                    }
                }
            }
        })
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        val series = (model.detailState.value as? SuccessState)?.data?.series

        if (series != null && item is EpisodeAdapter.EpisodeHolder) {
            val intent = Intent(activity, PlayerActivity::class.java)
            intent.putExtra(PlayerActivity.EXTRA_EPISODE, item.episode)
            intent.putExtra(PlayerActivity.EXTRA_SERIES, series)
            startActivity(intent)
        }
    }

    override fun onSelectListClicked(seriesRow: DetailsData) {
        requireFragmentManager().beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .add(android.R.id.content, SideMenuFragment.create(seriesRow.series))
            .addToBackStack(null)
            .commit()
    }

    override fun onPageSelected(seriesRow: DetailsData, selection: PageSelection) {
        // load selected episode page
        val page = selection.pageNumber - 1
        if (page != seriesRow.currentPage) {
            model.selectPage(page)
        }
    }

    companion object {
        private const val MAX_TRANSITION_DELAY = 4000L
    }
}