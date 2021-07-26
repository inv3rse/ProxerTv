package com.inverse.unofficial.proxertv.ui.player

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.inverse.unofficial.proxertv.base.ProxerRepository
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.model.Episode
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.Stream
import com.inverse.unofficial.proxertv.ui.util.ErrorState
import com.inverse.unofficial.proxertv.ui.util.LoadContentErrorState
import com.inverse.unofficial.proxertv.ui.util.LoadingState
import com.inverse.unofficial.proxertv.ui.util.SuccessState
import com.inverse.unofficial.proxertv.ui.util.extensions.toV2
import io.reactivex.Completable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the [PlayerActivity]
 */
class PlayerViewModel @Inject constructor(
    private val client: ProxerClient,
    private val repository: ProxerRepository
) : ViewModel() {

    val streams = MutableLiveData<LoadContentErrorState<List<Stream>>>()

    private val disposables = CompositeDisposable()

    private var series: Series? = null
    private var episode: Episode? = null

    fun init(series: Series, episode: Episode) {
        if (this.series == series && this.episode == episode) {
            return
        }

        this.series = series
        this.episode = episode

        loadStreams(series.id, episode)
    }

    fun loadStreams(seriesId: Long, episode: Episode) {
        disposables.clear()
        streams.value = LoadingState()

        client.loadEpisodeStreams(seriesId, episode.episodeNum, episode.languageType).toV2()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Stream> {

                private val streamList = mutableListOf<Stream>()

                override fun onSubscribe(d: Disposable) {
                    disposables.add(d)
                }

                override fun onNext(stream: Stream) {
                    streamList.add(stream)
                    streams.value = SuccessState(streamList)
                }

                override fun onComplete() {
                    if (streamList.isEmpty()) {
                        streams.value = ErrorState(error = NoSupportedStreamException())
                    }
                }

                override fun onError(e: Throwable) {
                    Timber.e(e)
                    if (streamList.isEmpty()) {
                        streams.value = ErrorState()
                    }
                }
            })
    }

    @SuppressLint("CheckResult")
    fun markAsWatched() {
        val seriesId = series?.id ?: return
        val episodeNum = episode?.episodeNum ?: return

        repository.getSeriesProgress(seriesId).toV2()
            .firstOrError()
            .flatMapCompletable { progress ->
                if (progress < episodeNum) {
                    repository.setSeriesProgress(seriesId, episodeNum).toV2().ignoreElements()
                } else {
                    Completable.complete()
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    Timber.e(it)
                }
            )
    }

    override fun onCleared() {
        disposables.dispose()
    }

    class NoSupportedStreamException: Exception()
}