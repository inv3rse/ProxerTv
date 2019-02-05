package com.inverse.unofficial.proxertv.ui.details

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.ui.player.PlayerActivity
import com.inverse.unofficial.proxertv.ui.util.*
import com.inverse.unofficial.proxertv.ui.util.extensions.provideViewModel

/**
 * The details view for a series.
 */
class SeriesDetailsFragment : DetailsSupportFragment(), OnItemViewClickedListener,
    SeriesDetailsRowPresenter.SeriesDetailsRowListener {

    private lateinit var detailsAdapter: DetailsAdapter
    private lateinit var episodePresenter: EpisodePresenter

    private lateinit var model: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().postponeEnterTransition()
        val handler = Handler()
        handler.postDelayed({ activity?.startPostponedEnterTransition() }, MAX_TRANSITION_DELAY)

        val seriesId = activity?.intent?.extras?.getInt(DetailsActivity.EXTRA_SERIES_ID)
            ?: throw IllegalArgumentException("seriesId must be set")

        val glide = GlideApp.with(this)
        detailsAdapter = DetailsAdapter(glide, this, object : CoverLoadListener {
            override fun onCoverLoadFailed() {
                activity?.startPostponedEnterTransition()
                handler.removeCallbacksAndMessages(null)
            }

            override fun onCoverLoadSuccess() {
                activity?.startPostponedEnterTransition()
                handler.removeCallbacksAndMessages(null)
            }
        })

        episodePresenter = EpisodePresenter(glide, seriesId)
        adapter = detailsAdapter
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
                is ErrorState -> {
                    requireActivity().startPostponedEnterTransition()
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
            val intent = PlayerActivity.createIntent(requireContext(), series, item.episode)
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