package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItemsCount: Int = 5,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current

    val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    
    // Empty items for padding at top and bottom
    val paddingItemsCount = visibleItemsCount / 2
    val paddedItems = List(paddingItemsCount) { null } + items + List(paddingItemsCount) { null }

    LaunchedEffect(initialIndex) {
        if (listState.firstVisibleItemIndex != initialIndex) {
            listState.scrollToItem(initialIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) {
                    onItemSelected(items[index])
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleItemsCount)
    ) {
        items(paddedItems.size) { index ->
            val item = paddedItems[index]
            val isSelected = index == listState.firstVisibleItemIndex + paddingItemsCount
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    val alpha by remember(listState) {
                        derivedStateOf {
                            val viewIndex = listState.firstVisibleItemIndex + paddingItemsCount
                            val diff = kotlin.math.abs(index - viewIndex)
                            when (diff) {
                                0 -> 1f
                                1 -> 0.5f
                                2 -> 0.2f
                                else -> 0.1f
                            }
                        }
                    }
                    val scale by remember(listState) {
                        derivedStateOf {
                            val viewIndex = listState.firstVisibleItemIndex + paddingItemsCount
                            val diff = kotlin.math.abs(index - viewIndex)
                            when (diff) {
                                0 -> 1.1f
                                1 -> 0.9f
                                2 -> 0.8f
                                else -> 0.7f
                            }
                        }
                    }
                    Box(modifier = Modifier
                        .alpha(alpha)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }) {
                        itemContent(item, isSelected)
                    }
                }
            }
        }
    }
}
