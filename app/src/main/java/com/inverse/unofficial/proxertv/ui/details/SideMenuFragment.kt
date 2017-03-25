package com.inverse.unofficial.proxertv.ui.details

import android.app.Fragment
import android.os.Bundle
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ItemBridgeAdapter
import android.support.v17.leanback.widget.VerticalGridView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.inverse.unofficial.proxertv.R

/**
 * Fragment to show a right side option list. The let side is transparent to keep the focus from getting lost.
 */
class SideMenuFragment : Fragment() {

    private lateinit var gridView: VerticalGridView
    private val focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, _ ->
        if (!gridView.hasFocus()) {
            fragmentManager.beginTransaction().remove(this@SideMenuFragment).commit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_side_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gridView = view.findViewById(R.id.side_menu_items_grid) as VerticalGridView

        val optionsAdapter = ArrayObjectAdapter(SeriesListOptionPresenter())
        optionsAdapter.addAll(0, listOf(1, 2, 3, 4, 5))

        val bridgeAdapter = ItemBridgeAdapter(optionsAdapter)
        gridView.adapter = bridgeAdapter

        view.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
    }

    override fun onDestroyView() {
        view.viewTreeObserver.removeOnGlobalFocusChangeListener(focusListener)
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        gridView.requestFocus()
    }
}