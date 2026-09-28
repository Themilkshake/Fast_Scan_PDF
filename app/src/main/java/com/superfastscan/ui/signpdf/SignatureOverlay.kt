package com.superfastscan.ui.signpdf

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Ultra-smooth, high-performance draggable & resizable signature overlay.
 *
 * Reliably maintains drag responsiveness across scale adjustments, slider updates,
 * and page navigation. Supports dragging beyond page boundaries.
 */
@Composable
fun SignatureOverlay(
    signatureBitmap: Bitmap,
    pageWidthDp: Dp,
    pageHeightDp: Dp,
    normOffsetX: Float,
    normOffsetY: Float,
    scale: Float,
    onNormalizedPositionChanged: (Float, Float) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pageWidthPx = with(density) { pageWidthDp.toPx() }
    val pageHeightPx = with(density) { pageHeightDp.toPx() }

    // Base signature width is 40% of page width at scale 1.0
    val baseWidthDp = pageWidthDp * 0.40f
    val baseWidthPx = with(density) { baseWidthDp.toPx() }

    // Local mutable scale synced with incoming scale parameter
    var currentScale by remember { mutableFloatStateOf(scale) }
    LaunchedEffect(scale) {
        currentScale = scale
    }

    val sigWidthDp = baseWidthDp * currentScale
    val sigAspect = signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat().coerceAtLeast(1f)
    val sigHeightDp = sigWidthDp * sigAspect

    // Local mutable pixel position synced with normOffsetX/normOffsetY
    var currentPixelX by remember { mutableFloatStateOf(normOffsetX * pageWidthPx) }
    var currentPixelY by remember { mutableFloatStateOf(normOffsetY * pageHeightPx) }

    LaunchedEffect(normOffsetX, normOffsetY, pageWidthPx, pageHeightPx) {
        currentPixelX = normOffsetX * pageWidthPx
        currentPixelY = normOffsetY * pageHeightPx
    }

    // Capture latest lambdas without resetting pointerInput
    val currentOnPositionChanged by rememberUpdatedState(onNormalizedPositionChanged)
    val currentOnScaleChanged by rememberUpdatedState(onScaleChanged)

    // Memoize the ImageBitmap conversion
    val memoizedSignatureBitmap = remember(signatureBitmap) {
        signatureBitmap.asImageBitmap()
    }

    Box(
        modifier = modifier
            .offset { IntOffset(currentPixelX.roundToInt(), currentPixelY.roundToInt()) }
            .size(sigWidthDp, sigHeightDp)
            .border(
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                RoundedCornerShape(6.dp)
            )
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                RoundedCornerShape(6.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val newNormX = if (pageWidthPx > 0) currentPixelX / pageWidthPx else 0f
                        val newNormY = if (pageHeightPx > 0) currentPixelY / pageHeightPx else 0f
                        currentOnPositionChanged(newNormX, newNormY)
                    },
                    onDragCancel = {
                        val newNormX = if (pageWidthPx > 0) currentPixelX / pageWidthPx else 0f
                        val newNormY = if (pageHeightPx > 0) currentPixelY / pageHeightPx else 0f
                        currentOnPositionChanged(newNormX, newNormY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        // Dynamically compute live dimensions
                        val liveSigWidthPx = baseWidthPx * currentScale
                        val liveSigHeightPx = liveSigWidthPx * sigAspect

                        // Allow dragging up to 80% of signature outside page boundaries
                        val minPixelX = -liveSigWidthPx * 0.80f
                        val maxPixelX = pageWidthPx - liveSigWidthPx * 0.20f
                        val minPixelY = -liveSigHeightPx * 0.80f
                        val maxPixelY = pageHeightPx - liveSigHeightPx * 0.20f

                        currentPixelX = (currentPixelX + dragAmount.x).coerceIn(minPixelX, maxPixelX)
                        currentPixelY = (currentPixelY + dragAmount.y).coerceIn(minPixelY, maxPixelY)
                    }
                )
            }
    ) {
        // Signature Image
        Image(
            bitmap = memoizedSignatureBitmap,
            contentDescription = "Signature",
            modifier = Modifier.size(sigWidthDp, sigHeightDp),
            contentScale = ContentScale.FillBounds
        )

        // ── Delete Button (Top-Right) ──
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(26.dp)
                .shadow(2.dp, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove signature",
                modifier = Modifier.size(15.dp)
            )
        }

        // ── Corner Resize Handle (Bottom-Right) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
                .size(34.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            currentOnScaleChanged(currentScale)
                        },
                        onDragCancel = {
                            currentOnScaleChanged(currentScale)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (baseWidthPx > 0) {
                                val delta = (dragAmount.x + dragAmount.y) / (baseWidthPx * 0.65f)
                                currentScale = (currentScale + delta).coerceIn(0.15f, 2.5f)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Resize signature",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
