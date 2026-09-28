package com.superfastscan.ui.watermarkpdf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superfastscan.domain.usecase.WatermarkConfig
import com.superfastscan.domain.usecase.WatermarkType
import kotlin.math.roundToInt

/**
 * Interactive Watermark Overlay Widget for the PDF Live Preview.
 *
 * Provides real-time rendering of text and image watermarks with live drag positioning,
 * rotation, opacity, and scaling synchronized with normalized document coordinates.
 */
@Composable
fun WatermarkOverlay(
    config: WatermarkConfig,
    containerWidthDp: Dp,
    containerHeightDp: Dp,
    onPositionChanged: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val containerWidthPx = with(density) { containerWidthDp.toPx() }
    val containerHeightPx = with(density) { containerHeightDp.toPx() }

    var elementSize by remember { mutableStateOf(IntSize.Zero) }

    var centerPxX by remember(config.normalizedX, containerWidthPx) {
        mutableFloatStateOf(config.normalizedX * containerWidthPx)
    }
    var centerPxY by remember(config.normalizedY, containerHeightPx) {
        mutableFloatStateOf(config.normalizedY * containerHeightPx)
    }

    val memoizedImageBitmap = remember(config.imageBitmap) {
        config.imageBitmap?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .offset {
                val x = (centerPxX - elementSize.width / 2f).coerceIn(0f, (containerWidthPx - elementSize.width).coerceAtLeast(0f))
                val y = (centerPxY - elementSize.height / 2f).coerceIn(0f, (containerHeightPx - elementSize.height).coerceAtLeast(0f))
                IntOffset(x.roundToInt(), y.roundToInt())
            }
            .onSizeChanged { elementSize = it }
            .graphicsLayer {
                rotationZ = config.rotation
                alpha = config.opacity.coerceIn(0.05f, 1.0f)
            }
            .pointerInput(containerWidthPx, containerHeightPx) {
                detectDragGestures(
                    onDragEnd = {
                        val newNormX = if (containerWidthPx > 0) (centerPxX / containerWidthPx).coerceIn(0f, 1f) else 0.5f
                        val newNormY = if (containerHeightPx > 0) (centerPxY / containerHeightPx).coerceIn(0f, 1f) else 0.5f
                        onPositionChanged(newNormX, newNormY)
                    },
                    onDragCancel = {
                        val newNormX = if (containerWidthPx > 0) (centerPxX / containerWidthPx).coerceIn(0f, 1f) else 0.5f
                        val newNormY = if (containerHeightPx > 0) (centerPxY / containerHeightPx).coerceIn(0f, 1f) else 0.5f
                        onPositionChanged(newNormX, newNormY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        centerPxX = (centerPxX + dragAmount.x).coerceIn(10f, (containerWidthPx - 10f).coerceAtLeast(10f))
                        centerPxY = (centerPxY + dragAmount.y).coerceIn(10f, (containerHeightPx - 10f).coerceAtLeast(10f))
                    }
                )
            }
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                RoundedCornerShape(6.dp)
            )
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (config.type == WatermarkType.TEXT) {
            val baseFontSize = with(density) { (containerWidthDp * 0.07f).toSp() }
            val fontSize = (baseFontSize.value * config.scale.coerceIn(0.2f, 4.0f)).sp

            Text(
                text = config.text.ifBlank { "WATERMARK" },
                fontSize = fontSize,
                fontWeight = if (config.isBold) FontWeight.Bold else FontWeight.Normal,
                color = Color(config.textColor),
                textAlign = TextAlign.Center
            )
        } else if (config.type == WatermarkType.IMAGE && memoizedImageBitmap != null) {
            val baseImgWidthDp = containerWidthDp * 0.45f
            val imgWidthDp = baseImgWidthDp * config.scale.coerceIn(0.2f, 4.0f)
            val aspect = config.imageBitmap!!.height.toFloat() / config.imageBitmap.width.toFloat().coerceAtLeast(1f)
            val imgHeightDp = imgWidthDp * aspect

            Image(
                bitmap = memoizedImageBitmap,
                contentDescription = "Watermark",
                modifier = Modifier.size(imgWidthDp, imgHeightDp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
