package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    visibleItemsCount: Int = 3,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    val isGlass = isLiquidGlassEnabled()
    val isLight = isAppLightMode()

    val initialIndex = remember(items, selectedItem) {
        items.indexOf(selectedItem).coerceAtLeast(0)
    }

    val paddingItemsCount = remember(visibleItemsCount) { visibleItemsCount / 2 }

    val paddedItems = remember(items, visibleItemsCount) {
        List(paddingItemsCount) { null } + items + List(paddingItemsCount) { null }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    
    // Explicit SnapLayoutInfoProvider ensures precise item centering lock
    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(lazyListState = listState)
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = snapLayoutInfoProvider)

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
                (indexInPadded - paddingItemsCount).coerceIn(0, (items.size - 1).coerceAtLeast(0))
            }
        }
    }

    // Sync with external changes to selectedItem
    LaunchedEffect(selectedItem, items) {
        val targetIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        if (targetIndex != centerIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Notify item selection whenever settling occurs or index changes
    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices && items[centerIndex] != selectedItem) {
            onItemSelected(items[centerIndex])
        }
    }

    // Ensure zero-offset snap when scroll settles
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                if (centerIndex in items.indices) {
                    listState.scrollToItem(centerIndex)
                }
            }
    }

    // Consume remaining scroll so parent doesn't scroll when wheel reaches ends
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available // Consume everything left
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .fillMaxWidth()
            .clipToBounds()
            .nestedScroll(nestedScrollConnection),
        contentAlignment = Alignment.Center
    ) {
        if (!isGlass) {
            // Material 3 Mode: Subtle tonal highlight with crisp top & bottom selection guide lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .padding(horizontal = 2.dp)
                    .background(
                        color = AccentCyan.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .drawBehind {
                        val strokePx = 1.dp.toPx()
                        val lineColor = AccentCyan.copy(alpha = 0.40f)
                        // Top selection guide line
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokePx
                        )
                        // Bottom selection guide line
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = strokePx
                        )
                    }
            )
        } else {
            // Liquid Glass Mode: Translucent glass highlight box with soft glowing border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .padding(horizontal = 2.dp),
                color = if (isLight) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isLight) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f)
                )
            ) {}
        }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(paddedItems.size) { index ->
                val item = paddedItems[index]
                val isSelected = (index - paddingItemsCount) == centerIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item != null) {
                        val viewIndex = centerIndex + paddingItemsCount
                        val diff = kotlin.math.abs(index - viewIndex)

                        val disableHeavy = isHeavyEffectsDisabled()
                        val alpha = when (diff) {
                            0 -> 1f
                            1 -> 0.45f
                            2 -> 0.18f
                            else -> 0f
                        }

                        val scale = when (diff) {
                            0 -> 1.0f
                            1 -> 0.88f
                            else -> 0.76f
                        }

                        val rotation = if (disableHeavy) 0f else {
                            val signedDiff = (index - viewIndex).toFloat()
                            (-signedDiff * 16f).coerceIn(-40f, 40f)
                        }

                        Box(
                            modifier = Modifier
                                .alpha(alpha)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    rotationX = rotation
                                    cameraDistance = 12f * density
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            itemContent(item, isSelected)
                        }
                    }
                }
            }
        }
    }
}
