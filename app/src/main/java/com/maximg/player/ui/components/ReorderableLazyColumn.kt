package com.maximg.player.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.maximg.player.util.move
import kotlin.math.abs

@Composable
fun <T : Any> ReorderableLazyColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable LazyItemScope.(index: Int, item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    var localItems by remember(items) { mutableStateOf(items.toList()) }

    LaunchedEffect(items) {
        if (draggingIndex == null) {
            localItems = items.toList()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        itemsIndexed(localItems, key = { _, item -> key(item) }) { index, item ->
            val isDragging = index == draggingIndex
            val itemOffsetY = if (isDragging) draggingOffsetY else 0f

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = itemOffsetY }
            ) {
                val thisDragModifier = Modifier.pointerInput(index) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { _ ->
                            draggingIndex = index
                            draggingOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingOffsetY += dragAmount.y
                            val currentDragging = draggingIndex
                                ?: return@detectDragGesturesAfterLongPress
                            val target =
                                calculateTargetIndex(currentDragging, draggingOffsetY, listState)
                            if (target != currentDragging && target in localItems.indices) {
                                val mutable = localItems.toMutableList()
                                mutable.move(currentDragging, target)
                                localItems = mutable
                                draggingIndex = target
                                draggingOffsetY = 0f
                            }
                        },
                        onDragEnd = {
                            val finalIndex = draggingIndex
                            if (finalIndex != null) {
                                val draggedKey = key(localItems[finalIndex])
                                val originalIndex = items.indexOfFirst { key(it) == draggedKey }
                                if (originalIndex != -1 && originalIndex != finalIndex) {
                                    onReorder(originalIndex, finalIndex)
                                }
                            }
                            draggingIndex = null
                            draggingOffsetY = 0f
                        },
                        onDragCancel = {
                            localItems = items.toList()
                            draggingIndex = null
                            draggingOffsetY = 0f
                        }
                    )
                }
                content(index, item, isDragging, thisDragModifier)
            }
        }
    }
}

private fun calculateTargetIndex(
    draggingIndex: Int,
    offsetY: Float,
    listState: LazyListState
): Int {
    val info = listState.layoutInfo.visibleItemsInfo
    val draggingInfo = info.firstOrNull { it.index == draggingIndex } ?: return draggingIndex
    val center = draggingInfo.offset + draggingInfo.size / 2f + offsetY
    return info
        .filter { it.index != draggingIndex }
        .minByOrNull { abs(it.offset + it.size / 2f - center) }
        ?.index ?: draggingIndex
}
