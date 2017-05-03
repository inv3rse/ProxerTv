package com.inverse.unofficial.proxertv.ui.details

import android.app.Fragment
import android.os.Bundle
import android.support.v17.leanback.widget.ItemBridgeAdapter
import android.support.v17.leanback.widget.VerticalGridView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.inverse.unofficial.proxertv.R
import com.inverse.unofficial.proxertv.base.App
import com.inverse.unofficial.proxertv.base.CrashReporting
import com.inverse.unofficial.proxertv.model.Series
import com.inverse.unofficial.proxertv.model.SeriesList
import org.jetbrains.anko.withArguments
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Fragment to show a right side option list. The let side is transparent to keep the focus from getting lost.
 */
class SideMenuFragment : Fragment(), SeriesListSelectionListener {
    private val repository = App.component.getProxerRepository()

    private val optionsAdapter = SeriesListOptionAdapter(this)
    private var listStateSubscription: Subscription? = null
    private var updateSubscription: Subscription? = null

    private lateinit var series: Series
    private lateinit var gridView: VerticalGridView

    private val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
        if (!gridView.hasFocus()) {
            fragmentManager.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        series = arguments.getParcelable(ARG_SERIES)

        listStateSubscription = repository.observerSeriesListState(series.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ state: SeriesList ->
                    optionsAdapter.currentState = state
                    optionsAdapter.notifyItemRangeChanged(0, optionsAdapter.size())
                })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_side_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gridView = view.findViewById(R.id.side_menu_items_grid) as VerticalGridView

        val bridgeAdapter = ItemBridgeAdapter(optionsAdapter)
        gridView.adapter = bridgeAdapter

        view.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
    }

    override fun onDestroyView() {
        view.viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
        super.onDestroyView()
    }

    override fun onDestroy() {
        listStateSubscription?.unsubscribe()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        gridView.requestFocus()
    }

    override fun listSelected(seriesList: SeriesList) {
        if (seriesList != optionsAdapter.currentState) {
            updateSubscription?.unsubscribe()

            optionsAdapter.loadingList = seriesList
            optionsAdapter.notifyItemRangeChanged(0, optionsAdapter.size())

            updateSubscription = repository.moveSeriesToList(series, seriesList)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        optionsAdapter.loadingList = null
                        optionsAdapter.notifyItemRangeChanged(0, optionsAdapter.size())
                    }, {
                        CrashReporting.logException(it)
                        optionsAdapter.loadingList = null
                        optionsAdapter.notifyItemRangeChanged(0, optionsAdapter.size())
                    })
        }
    }

    companion object {
        fun create(series: Series): SideMenuFragment {
            return SideMenuFragment().withArguments(
                    ARG_SERIES to series)
        }

        private const val ARG_SERIES = "ARG_SERIES"
    }
}