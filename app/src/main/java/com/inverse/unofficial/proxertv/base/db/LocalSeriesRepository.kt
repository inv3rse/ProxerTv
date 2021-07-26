package com.inverse.unofficial.proxertv.base.db

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.inverse.unofficial.proxertv.Database
import com.inverse.unofficial.proxertv.base.IODispatcher
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.SystemSeriesList
import com.inverse.unofficial.proxertv.model.SystemSeriesListEntry
import com.inverse.unofficial.proxertv.model.UserSeriesList
import com.inverse.unofficial.proxertv.sql.UserSeriesListEntry
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wrapper around the local database.
 */
@Reusable
class LocalSeriesRepository @Inject constructor(
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val database: Database
) {

    fun observeUserSeriesListEntries(list: UserSeriesList): Flow<List<UserSeriesListEntry>> {
        return database.userSeriesEntryQueries.queryEntriesByListType(list).asFlow().mapToList(ioDispatcher)
    }

    fun observeSystemSeriesList(list: SystemSeriesList): Flow<List<SystemSeriesListEntry>> {
        return database.systemSeriesEntryQueries.queryEntriesByListType(list, ::SystemSeriesListEntry).asFlow()
            .mapToList(ioDispatcher)
    }

    fun upsertSystemSeriesListEntries(list: SystemSeriesList, startPosition: Long, items: List<ISeriesCover>) {
        database.systemSeriesEntryQueries.withTransaction {
            items.forEachIndexed { index, item ->
                val position = startPosition + index
                upsertSeriesToList(position, list, item.id, item.name)
            }
        }
    }

    fun createSystemSeriesPagingSource(list: SystemSeriesList): PagingSource<Int, SystemSeriesListEntry> {
        return TopListPagingSource(database, list)
    }

    private fun <T : Transacter> T.withTransaction(noEnclosing: Boolean = false, body: T.() -> Unit) {
        transaction(noEnclosing) {
            this@withTransaction.body()
        }
    }

    /**
     * Key: orderPosition
     */
    private class TopListPagingSource constructor(
        private val database: Database,
        private val listType: SystemSeriesList,
    ) : PagingSource<Int, SystemSeriesListEntry>(), Query.Listener {

        private var currentQuery: Query<SystemSeriesListEntry>? by Delegates.observable(null) { _, old, new ->
            old?.removeListener(this)
            new?.addListener(this)
        }

        init {
            registerInvalidatedCallback {
                currentQuery?.removeListener(this)
                currentQuery = null
            }
        }

        override fun queryResultsChanged() {
//            Handler(Looper.getMainLooper()).postDelayed({invalidate()}, 1000)
            invalidate()
        }

        override fun getRefreshKey(state: PagingState<Int, SystemSeriesListEntry>): Int? {
            return state.anchorPosition?.let { ((it + 1) / 50) * 50 }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SystemSeriesListEntry> {
            return try {
                val key = params.key ?: 0
                database.systemSeriesEntryQueries.transactionWithResult {

                    val query = database.systemSeriesEntryQueries.queryEntriesByListTypeWindow(
                        listType,
                        key.toLong(),
                        (key + params.loadSize + 1).toLong(), ::SystemSeriesListEntry
                    )

                    currentQuery = query

                    val queryResult = query.executeAsList()
                    val data = queryResult.take(params.loadSize)

                    val page = LoadResult.Page(
                        data = data,
                        prevKey = if (key <= 0L) null else (key - params.loadSize).coerceAtLeast(0),
                        nextKey = if (queryResult.size > params.loadSize) queryResult.last().orderPosition.toInt() else null,
                        itemsBefore = maxOf(0, key),
                        itemsAfter = LoadResult.Page.COUNT_UNDEFINED
                    )

                    Timber.d("loaded $listType key $key {data: ${queryResult.size} prevKey: ${page.prevKey} nextKey: ${page.nextKey} itemsBefore: ${page.itemsBefore} itemsAfter: ${page.itemsAfter}}")
                    page
                }
            } catch (e: Exception) {
                if (e is IndexOutOfBoundsException) throw e
                LoadResult.Error(e)
            }
        }
    }
}