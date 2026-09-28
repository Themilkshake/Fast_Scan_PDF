package com.superfastscan.ui.mergepdf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.superfastscan.domain.model.PdfFileItem
import com.superfastscan.ui.mergepdf.components.PdfFileCard

/**
 * 2-Column Grid Drag-and-Drop component for PDF documents.
 *
 * Supports true multidirectional physical dragging:
 * - **Horizontal (Sağa-Sola)**: Drag right/left to swap adjacent column items.
 * - **Vertical (Yukarı-Aşağı)**: Drag up/down to swap row items.
 * - Floating scale & elevation animation while dragging.
 * - [Modifier.animateItem] for smooth neighbor placement animations.
 */
@Composable
fun ReorderablePdfList(
    items: List<PdfFileItem>,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onDeleteItem: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    // Drag state tracking
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Dynamic measurement of cell dimensions
    var cellWidthPx by remember { mutableFloatStateOf(0f) }
    var cellHeightPx by remember { mutableFloatStateOf(0f) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            val isCurrentDragging = draggingIndex == index

            val scale by animateFloatAsState(
                targetValue = if (isCurrentDragging) 1.06f else 1.0f,
                label = "scale"
            )

            PdfFileCard(
                fileItem = item,
                index = index,
                isDragging = isCurrentDragging,
                onDelete = { onDeleteItem(item.id) },
                modifier = Modifier
                    .animateItem()
                    .zIndex(if (isCurrentDragging) 5f else 1f)
                    .onGloballyPositioned { coordinates ->
                        if (cellWidthPx == 0f) {
                            cellWidthPx = coordinates.size.width.toFloat()
                            cellHeightPx = coordinates.size.height.toFloat()
                        }
                    }
                    .graphicsLayer {
                        if (isCurrentDragging) {
                            translationX = dragOffsetX
                            translationY = dragOffsetY
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    .pointerInput(items.size) {
                        // Supports continuous 1-finger drag & long-press drag
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y

                                val currentIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val effectiveCellW = if (cellWidthPx > 0) cellWidthPx else 300f
                                val effectiveCellH = if (cellHeightPx > 0) cellHeightPx else 400f

                                // ── 1. Horizontal Drag (Sağa-Sola Swapping) ──
                                val horizThreshold = effectiveCellW * 0.55f
                                if (dragOffsetX >= horizThreshold && currentIdx < items.size - 1) {
                                    // Dragging Right -> Swap with next item
                                    onMoveItem(currentIdx, currentIdx + 1)
                                    draggingIndex = currentIdx + 1
                                    dragOffsetX -= effectiveCellW
                                } else if (dragOffsetX <= -horizThreshold && currentIdx > 0) {
                                    // Dragging Left -> Swap with previous item
                                    onMoveItem(currentIdx, currentIdx - 1)
                                    draggingIndex = currentIdx - 1
                                    dragOffsetX += effectiveCellW
                                }

                                // ── 2. Vertical Drag (Yukarı-Aşağı Swapping) ──
                                val vertThreshold = effectiveCellH * 0.55f
                                if (dragOffsetY >= vertThreshold && currentIdx + 2 < items.size) {
                                    // Dragging Down -> Swap with item 2 positions ahead
                                    onMoveItem(currentIdx, currentIdx + 2)
                                    draggingIndex = currentIdx + 2
                                    dragOffsetY -= effectiveCellH
                                } else if (dragOffsetY <= -vertThreshold && currentIdx - 2 >= 0) {
                                    // Dragging Up -> Swap with item 2 positions back
                                    onMoveItem(currentIdx, currentIdx - 2)
                                    draggingIndex = currentIdx - 2
                                    dragOffsetY += effectiveCellH
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            }
                        )
                    }
            )
        }
    }
}
