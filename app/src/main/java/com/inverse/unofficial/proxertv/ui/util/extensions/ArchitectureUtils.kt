package com.inverse.unofficial.proxertv.ui.util.extensions

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Provide the ViewModel for the given [activity]. If it does not exists yet the [factory] will be used to create it.
 */
inline fun <reified T : ViewModel> provideViewModel(activity: FragmentActivity?, crossinline factory: () -> T): T {
    // nullable parameter to avoid the unsafe operator in fragments
    if (activity == null) {
        throw IllegalArgumentException("activity must not be null")
    }

    return ViewModelProvider(activity, object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return factory() as T
        }
    }).get(T::class.java)
}

/**
 * Provide the ViewModel for the given [fragment]. If it does not exists yet the [factory] will be used to create it.
 */
@Suppress("unused")
inline fun <reified T : ViewModel> provideViewModel(fragment: Fragment, crossinline factory: () -> T): T {
    return ViewModelProvider(fragment, object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return factory() as T
        }
    }).get(T::class.java)
}

/**
 * LiveData [Observer] that will only be invoked for non null values
 */
fun <T> nonNullObserver(block: (T) -> Unit): Observer<T> {
    return Observer { data ->
        if (data != null) {
            block(data)
        }
    }
}
