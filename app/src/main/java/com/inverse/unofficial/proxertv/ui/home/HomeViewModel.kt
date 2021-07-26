package com.inverse.unofficial.proxertv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.cachedIn
import com.inverse.unofficial.proxertv.base.ProxerRepository
import com.inverse.unofficial.proxertv.base.db.LocalSeriesRepository
import com.inverse.unofficial.proxertv.model.SystemSeriesList
import com.inverse.unofficial.proxertv.model.SystemSeriesListEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.yield
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the [MainFragment].
 */
class HomeViewModel @Inject constructor(
    private val proxerRepository: ProxerRepository,
    private val localSeriesRepository: LocalSeriesRepository,
) : ViewModel() {

    val topAccessSeriesList = createPagingFlow(SystemSeriesList.TOP_ACCESS)
    val topRatedSeriesList = createPagingFlow(SystemSeriesList.TOP_RATING)
    val topRatedMoviesList = createPagingFlow(SystemSeriesList.TOP_RATING_MOVIES)
    val airingSeriesList = createPagingFlow(SystemSeriesList.AIRING)

    private fun createPagingFlow(list: SystemSeriesList): Flow<PagingData<SystemSeriesListEntry>> {
        val pager = Pager(
            config = PagingConfig(pageSize = 50, initialLoadSize = 50, enablePlaceholders = false),
            remoteMediator = TopListRemoteMediator(list, proxerRepository, localSeriesRepository),
            pagingSourceFactory = { localSeriesRepository.createSystemSeriesPagingSource(list) })

        return pager.flow.onEach { Timber.d("emit new PagingData for $list") }
            .catch { Timber.e(it) }
            .cachedIn(viewModelScope)
    }

    class TopListRemoteMediator @Inject constructor(
        private val list: SystemSeriesList,
        private val proxerRepository: ProxerRepository,
        private val localSeriesRepository: LocalSeriesRepository
    ) : RemoteMediator<Int, SystemSeriesListEntry>() {

        override suspend fun initialize(): InitializeAction {
            return InitializeAction.LAUNCH_INITIAL_REFRESH
        }

        override suspend fun load(loadType: LoadType, state: PagingState<Int, SystemSeriesListEntry>): MediatorResult {
            try {
                val page = when (loadType) {
                    LoadType.REFRESH -> 1
                    LoadType.PREPEND -> {
                        val prevKey = state.pages.firstOrNull()?.prevKey ?: return MediatorResult.Success(true)
                        (prevKey / PAGE_SIZE) + 1
                    }
                    LoadType.APPEND -> {
                        val lastItem = state.lastItemOrNull()
                        lastItem ?: return MediatorResult.Success(true)
                        ((lastItem.orderPosition.plus(1) / PAGE_SIZE) + 1).toInt()
                    }
                }

                val pageItems = proxerRepository.loadSeriesList(list, page)

                localSeriesRepository.upsertSystemSeriesListEntries(list, (page - 1L) * PAGE_SIZE, pageItems)

                val endOfPaginationReached = (loadType == LoadType.PREPEND && page == 1) || pageItems.size < PAGE_SIZE
                if (endOfPaginationReached) {
                    Timber.e("endOfPaginationReached for $list on page $page")
                }
                return MediatorResult.Success(endOfPaginationReached)
            } catch (throwable: Throwable) {
                Timber.e(throwable)
                return MediatorResult.Error(throwable)
            }
        }

        companion object {
            const val PAGE_SIZE = 50
        }
    }
}