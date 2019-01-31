package com.inverse.unofficial.proxertv.ui.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.inverse.unofficial.proxertv.base.ProxerRepository
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.ui.util.ErrorState
import com.inverse.unofficial.proxertv.ui.util.LoadContentErrorState
import com.inverse.unofficial.proxertv.ui.util.LoadingState
import com.inverse.unofficial.proxertv.ui.util.SuccessState
import com.inverse.unofficial.proxertv.ui.util.extensions.toV2
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the [SeriesDetailsFragment]
 */
class DetailsViewModel @Inject constructor(
    private val proxerRepository: ProxerRepository
) : ViewModel() {

    val detailState = MutableLiveData<LoadContentErrorState<DetailsData>>()
    val episodesState = MutableLiveData<LoadContentErrorState<List<EpisodeCategory>>>()

    private var seriesId = -1
    private val disposables = CompositeDisposable()

    private val pageSubject = BehaviorSubject.createDefault(-1)

    /**
     * Initialize the ViewModel for [seriesId] if it has not been done.
     */
    fun init(seriesId: Int) {
        if (this.seriesId == seriesId) {
            return
        }

        this.seriesId = seriesId
        loadContent(seriesId)
    }

    /**
     * Try to load the content.
     */
    fun loadContent(seriesId: Int) {
        disposables.clear()

        detailState.value = LoadingState()
        episodesState.value = LoadingState()

        val progressObservable = proxerRepository.observeSeriesProgress(seriesId).toV2()
            .subscribeOn(Schedulers.io())
            .replay(1)

        Observables
            .combineLatest(
                proxerRepository.loadSeries(seriesId).toV2(),
                proxerRepository.observerSeriesListState(seriesId).toV2(),
                progressObservable,
                pageSubject
            ) { series, list, progress, page -> DetailsData(series, list, progress, currentPage(page, progress)) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    detailState.value = SuccessState(it)
                },
                onError = {
                    Timber.e(it)
                    detailState.value = ErrorState()
                }
            )
            .addTo(disposables)


        Observables
            .combineLatest(
                progressObservable.firstOrError().toObservable(),
                pageSubject.observeOn(Schedulers.io())
            )
            .flatMap { (progress, page) ->
                val loadPage = currentPage(page, progress)

                Observables.combineLatest(
                    proxerRepository.loadEpisodesPage(seriesId, loadPage).toV2(),
                    progressObservable
                ).takeUntil(pageSubject.skip(1))
            }
            .map { (episodesMap, progress) ->
                episodesMap.toList().map { EpisodeCategory(it.first, it.second, progress) }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    episodesState.value = SuccessState(it)
                },
                onError = {
                    Timber.e(it)
                    episodesState.value = ErrorState()
                }
            )
            .addTo(disposables)


        progressObservable.connect().addTo(disposables)
    }

    private fun currentPage(page: Int, progress: Int): Int {
        return if (page == -1) {
            ProxerClient.getTargetPageForEpisode(progress + 1)
        } else {
            page
        }
    }

    /**
     * Select the [page] (0 Indexed, 0 -> First Page)
     */
    fun selectPage(page: Int) {
        episodesState.value = LoadingState()
        pageSubject.onNext(page)
    }

    override fun onCleared() {
        disposables.dispose()
    }
}