package com.inverse.unofficial.proxertv.ui.home

import android.content.res.Resources
import android.os.Handler
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import androidx.annotation.StringRes
import androidx.leanback.widget.*
import com.inverse.unofficial.proxertv.model.ISeriesCover
import com.inverse.unofficial.proxertv.model.SeriesCover
import com.inverse.unofficial.proxertv.ui.util.LoadingCover
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

/**
 * Helper class for loading and updating a list of rows
 */
class RowsHelper(private val rowsAdapter: ArrayObjectAdapter, private val resources: Resources) {

    private val handler = Handler()
    private val subscriptions = CompositeSubscription()

    // paging state and observable providers
    private val pageProviders = mutableMapOf<Int, (Int) -> Observable<out List<ISeriesCover>>>()
    private val pageProgress = SparseIntArray()
    private val pagingEnabled = SparseBooleanArray()

    // access to ListRow and corresponding adapter based on target position
    private val rowTargetMap = mutableMapOf<ListRow, Int>()
    private val targetRowMap = mutableMapOf<Int, ObjectAdapter>()

    /**
     * Adds a row to the existing rows according to targetPos.
     * @param adapter the adapter to add
     * @param headerName the name of the header
     * @param targetPos the target index of the row. Must be unique but can have missing indices.
     */
    fun addRow(adapter: ObjectAdapter, @StringRes headerName: Int, targetPos: Int) {
        val listRow = ListRow(targetPos.toLong(), HeaderItem(resources.getString(headerName)), adapter)

        var targetIndex = rowsAdapter.size()
        // iterate over the existing rows to find the correct index for our targetPos
        // (the last row might be the only one added yet)
        for (i in 0 until rowsAdapter.size()) {
            val rowTarget = rowTargetMap[rowsAdapter.get(i)] ?: 0
            if (targetPos < rowTarget) {
                targetIndex = i
                break
            }
        }

        rowsAdapter.add(targetIndex, listRow)
        rowTargetMap[listRow] = targetPos
        targetRowMap[targetPos] = adapter
    }

    /**
     * Adds a observable row to the existing rows according to targetPos.
     * Subscribes to the loadObservable and sets the row content once it emits items. If the onNext List is empty or the
     * observable throws an error the row will be removed.
     * @param adapter the adapter to use
     * @param observable the observable to subscribe to for updates
     * @param headerName the name of the header
     * @param targetPos the target index of the row. Must be unique but can have missing indices.
     */
    fun addObservableRow(
        adapter: ArrayObjectAdapter, observable: Observable<out List<ISeriesCover>>,
        @StringRes headerName: Int, targetPos: Int
    ) {

        if (targetRowMap[targetPos] == null) {
            // add the row with a LoadingCover first
            adapter.add(LoadingCover)
            addRow(adapter, headerName, targetPos)
        }

        subscriptions.add(observable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { seriesList ->
                    val existingAdapter = targetRowMap[targetPos]
                    pageProgress.put(targetPos, 1)
                    pagingEnabled.put(targetPos, pageProviders.contains(targetPos))

                    if (existingAdapter != null && existingAdapter is ArrayObjectAdapter) {
                        if (!seriesList.isEmpty()) {
                            // update existing content
                            existingAdapter.clear()
                            existingAdapter.addAll(0, seriesList)
                        } else {
                            // list is empty, remove it
                            removeRow(targetPos)
                        }
                    } else if (seriesList.isNotEmpty()) {
                        adapter.clear()
                        adapter.addAll(0, seriesList)
                        addRow(adapter, headerName, targetPos)
                    }
                },
                { error ->
                    removeRow(targetPos)
                    Timber.e(error)
                }
            ))
    }

    /**
     * Adds a observable paging row to the existing rows according to targetPos.
     * @param adapter the adapter to use
     * @param pageObservableFactory provider of the page observable according to page number (first page is 1)
     * @param headerName the name of the header
     * @param targetPos the target index of the row. Must be unique but can have missing indices.
     */
    fun addObservablePagingRow(
        adapter: ArrayObjectAdapter, pageObservableFactory: (Int) -> Observable<out List<SeriesCover>>,
        @StringRes headerName: Int, targetPos: Int
    ) {

        pageProviders[targetPos] = pageObservableFactory
        addObservableRow(adapter, pageObservableFactory(DEFAULT_PAGE), headerName, targetPos)
    }

    /**
     * React to item selection
     */
    fun onItemSelected(selectedIndex: Int, rowAdapter: ArrayObjectAdapter, row: Row) {
        val targetPos = row.id.toInt()

        if (targetPos >= 0 && pagingEnabled[targetPos] && selectedIndex >= 0
            && selectedIndex > (rowAdapter.size() - START_PAGING_ITEMS_LEFT)
        ) {

            val pageProvider = pageProviders[targetPos]
            if (pageProvider != null) {
                pagingEnabled.put(targetPos, false)
                val nextPage = pageProgress.get(targetPos, DEFAULT_PAGE) + 1

                // delay the insertion, because the RecyclerView might be in a scrolling state
                handler.post {
                    rowAdapter.add(LoadingCover)
                    subscriptions.add(
                        pageProvider(nextPage)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { newPageItems ->
                                    if (newPageItems.isNotEmpty() && rowAdapter.size() > 0) {
                                        rowAdapter.replace(rowAdapter.size() - 1, newPageItems.first())
                                        rowAdapter.addAll(rowAdapter.size(), newPageItems.subList(1, newPageItems.size))
                                    } else {
                                        rowAdapter.remove(LoadingCover)
                                        rowAdapter.addAll(rowAdapter.size(), newPageItems)
                                    }

                                    pageProgress.put(targetPos, nextPage)
                                    pagingEnabled.put(targetPos, newPageItems.isNotEmpty())
                                },
                                { Timber.e(it) })
                    )
                }
            }
        }
    }

    /**
     * Cancel all pending operations.
     */
    fun cancelPendingOperations() {
        subscriptions.clear()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Removes the row for targetPos
     */
    private fun removeRow(targetPos: Int) {
        rowTargetMap.filterValues { it == targetPos }.forEach { entry ->
            rowsAdapter.remove(entry.key)
            rowTargetMap.remove(entry.key)
            targetRowMap.remove(entry.value)
        }
    }

    companion object {
        private const val DEFAULT_PAGE = 1
        private const val START_PAGING_ITEMS_LEFT = 4
    }
}