package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory scroll position cache that preserves exact LazyListState and ScrollState
 * offsets across parent/child navigation, sub-page transitions, and full-screen modals.
 */
object ScrollPositionStore {
    private val lazyListPositions = ConcurrentHashMap<String, Pair<Int, Int>>()
    private val scrollValues = ConcurrentHashMap<String, Int>()

    fun getLazyPosition(key: String): Pair<Int, Int> = lazyListPositions[key] ?: (0 to 0)

    fun saveLazyPosition(key: String, index: Int, offset: Int) {
        lazyListPositions[key] = index.coerceAtLeast(0) to offset.coerceAtLeast(0)
    }

    fun getScrollValue(key: String): Int = scrollValues[key] ?: 0

    fun saveScrollValue(key: String, value: Int) {
        scrollValues[key] = value.coerceAtLeast(0)
    }

    fun clear(key: String) {
        lazyListPositions.remove(key)
        scrollValues.remove(key)
    }
}

@Composable
fun rememberPersistedLazyListState(key: String): LazyListState {
    val state = remember(key) {
        val (savedIndex, savedOffset) = ScrollPositionStore.getLazyPosition(key)
        LazyListState(
            firstVisibleItemIndex = savedIndex,
            firstVisibleItemScrollOffset = savedOffset
        )
    }

    LaunchedEffect(key, state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                ScrollPositionStore.saveLazyPosition(key, index, offset)
            }
    }

    DisposableEffect(key, state) {
        onDispose {
            ScrollPositionStore.saveLazyPosition(
                key,
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset
            )
        }
    }

    return state
}

@Composable
fun rememberPersistedScrollState(key: String): ScrollState {
    val state = remember(key) {
        ScrollState(initial = ScrollPositionStore.getScrollValue(key))
    }

    LaunchedEffect(key, state) {
        snapshotFlow { state.value }
            .collectLatest { value ->
                ScrollPositionStore.saveScrollValue(key, value)
            }
    }

    DisposableEffect(key, state) {
        onDispose {
            ScrollPositionStore.saveScrollValue(key, state.value)
        }
    }

    return state
}
