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
    val initialIndex = remember(items, selectedItem) {
        items.indexOf(selectedItem).coerceAtLeast(0)
    }

    val paddingItemsCount = remember(visibleItemsCount) { visibleItemsCount / 2 }

    val paddedItems = remember(items, visibleItemsCount) {
        List(paddingItemsCount) { null } + items + List(paddingItemsCount) { null }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember(listState, items.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) 0
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closestToCenter = visibleItemsInfo.minByOrNull { 
                    val itemCenter = it.offset + it.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                val indexInPadded = closestToCenter?.index ?: 0
                (indexInPadded - paddingItemsCount).coerceIn(0, items.size - 1)
            }
        }
    }

    // Sync with external changes to selectedItem
    LaunchedEffect(selectedItem) {
        val targetIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        if (targetIndex != centerIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
            onItemSelected(items[centerIndex])
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(itemHeight * visibleItemsCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(paddedItems.size) { index ->
            val item = paddedItems[index]
            val paddingItemsCount = visibleItemsCount / 2
            val isSelected = index - paddingItemsCount == centerIndex
            
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
