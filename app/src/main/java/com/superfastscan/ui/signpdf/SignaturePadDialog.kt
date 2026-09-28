package com.superfastscan.ui.signpdf

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superfastscan.R

/**
 * Data class representing an individual stroke with its path, color, and thickness.
 */
data class SignatureStroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

// Available signature ink colors
val SignatureColors = listOf(
    Color(0xFF000000), // Black
    Color(0xFF0D47A1), // Navy Blue (Official signature color)
    Color(0xFF1976D2), // Royal Blue
    Color(0xFFC62828), // Deep Red
    Color(0xFF1B5E20)  // Forest Green
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureSaved: (Bitmap) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Store all completed strokes with their color & stroke width
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    // Current in-progress stroke path
    var currentPath by remember { mutableStateOf<Path?>(null) }

    // Selected color and stroke thickness
    var selectedColor by remember { mutableStateOf(SignatureColors[0]) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(5f) }

    // Canvas dimensions for high-res bitmap capture
    val canvasWidthPx = 1000
    val canvasHeightPx = 500

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = if (isLandscape) 16.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = stringResource(R.string.sign_draw_title),
                style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isLandscape) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sign_draw_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

            // ── Color & Thickness Palette ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Picker Swatches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignatureColors.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(if (isLandscape) 28.dp else 34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                                            CircleShape
                                        )
                                    } else {
                                        Modifier.border(
                                            BorderStroke(1.dp, Color.LightGray),
                                            CircleShape
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isLandscape) 14.dp else 18.dp)
                                )
                            }
                        }
                    }
                }

                // Stroke Width Picker (Thin, Medium, Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val widths = listOf(3f to 6.dp, 5f to 10.dp, 8f to 14.dp)
                    widths.forEach { (widthValue, dotSize) ->
                        val isSelected = selectedStrokeWidth == widthValue
                        Box(
                            modifier = Modifier
                                .size(if (isLandscape) 28.dp else 34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedStrokeWidth = widthValue },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

            // ── Drawing Canvas with instant dot + drag capture ──
            val canvasHeight = if (isLandscape) 120.dp else 200.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
                    .clipToBounds()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(canvasHeight)
                        .pointerInput(selectedColor, selectedStrokeWidth) {
                            awaitEachGesture {
                                // 1. Detect touch down
                                val down = awaitFirstDown()
                                val start = down.position

                                // Create path with an initial micro-step so even a single tap leaves a perfect dot!
                                val path = Path().apply {
                                    moveTo(start.x, start.y)
                                    lineTo(start.x + 0.1f, start.y + 0.1f)
                                }
                                currentPath = path

                                // 2. Track finger movement
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change != null && change.pressed) {
                                        if (change.positionChanged()) {
                                            change.consume()
                                            path.lineTo(change.position.x, change.position.y)
                                            // Force recomposition
                                            currentPath = Path().apply { addPath(path) }
                                        }
                                    } else {
                                        break
                                    }
                                } while (event.changes.any { it.pressed })

                                // 3. Finger lifted: store stroke
                                currentPath?.let {
                                    strokes.add(
                                        SignatureStroke(
                                            path = it,
                                            color = selectedColor,
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                currentPath = null
                            }
                        }
                ) {
                    // Draw completed strokes in their respective colors & thicknesses
                    for (stroke in strokes) {
                        drawPath(
                            path = stroke.path,
                            color = stroke.color,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw current in-progress stroke
                    currentPath?.let { path ->
                        drawPath(
                            path = path,
                            color = selectedColor,
                            style = Stroke(
                                width = selectedStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 10.dp else 18.dp))

            // ── Action Buttons (Clear & Done) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Clear button
                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        currentPath = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sign_clear),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Done button
                Button(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            val bitmap = captureSignatureBitmap(
                                strokes = strokes.toList(),
                                width = canvasWidthPx,
                                height = canvasHeightPx
                            )
                            onSignatureSaved(bitmap)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = strokes.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.sign_done),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Captures all drawn strokes with their respective colors and thicknesses
 * onto a transparent ARGB_8888 Bitmap.
 */
private fun captureSignatureBitmap(
    strokes: List<SignatureStroke>,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Render each stroke with its selected color and proportional thickness
    for (stroke in strokes) {
        val paint = android.graphics.Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth * 1.8f
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        val androidPath = stroke.path.asAndroidPath()
        canvas.drawPath(androidPath, paint)
    }

    return bitmap
}
