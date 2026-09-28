package com.superfastscan.ui.signpdf

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.superfastscan.R
import kotlin.math.roundToInt

/**
 * "Sign PDF" screen content with exact aspect ratio calibration.
 *
 * Prevents edge cropping and ensures 100% positional fidelity for signatures.
 */
@Composable
fun SignYourselfTab(
    uiState: SignPdfUiState,
    onSelectPdf: () -> Unit,
    onAddSignature: () -> Unit,
    onSignaturePositionChanged: (Float, Float) -> Unit,
    onSignatureScaleChanged: (Float) -> Unit,
    onRemoveSignature: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSaveDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (uiState.pageBitmap != null) {
        val pageBitmap = uiState.pageBitmap

        if (isLandscape) {
            // ────────────────────────────────────────────────
            // LANDSCAPE (MANZARA) LAYOUT: Side-by-Side (Row)
            // ────────────────────────────────────────────────
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: PDF Viewer container with exact aspect ratio sizing
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    val pageAspect = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
                    val containerAspect = containerWidth / containerHeight

                    val (pageDisplayWidth, pageDisplayHeight) = if (pageAspect > containerAspect) {
                        containerWidth to (containerWidth / pageAspect)
                    } else {
                        (containerHeight * pageAspect) to containerHeight
                    }

                    // PDF Page Box — EXACT aspect ratio, ZERO cropping!
                    val memoizedPageBitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() }
                    Box(
                        modifier = Modifier
                            .size(pageDisplayWidth, pageDisplayHeight)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .background(Color.White)
                    ) {
                        Image(
                            bitmap = memoizedPageBitmap,
                            contentDescription = "PDF Page ${uiState.currentPage + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        if (uiState.isSignaturePlaced && uiState.signatureBitmap != null) {
                            SignatureOverlay(
                                signatureBitmap = uiState.signatureBitmap,
                                pageWidthDp = pageDisplayWidth,
                                pageHeightDp = pageDisplayHeight,
                                normOffsetX = uiState.normOffsetX,
                                normOffsetY = uiState.normOffsetY,
                                scale = uiState.signatureScale,
                                onNormalizedPositionChanged = onSignaturePositionChanged,
                                onScaleChanged = onSignatureScaleChanged,
                                onDelete = onRemoveSignature
                            )
                        }
                    }
                }

                // Right: Controls Panel (Scrollable)
                val rightScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rightScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Navigation (if multi-page)
                    if (uiState.pageCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onPreviousPage,
                                enabled = uiState.currentPage > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
                            }
                            Text(
                                text = "${uiState.currentPage + 1} / ${uiState.pageCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = onNextPage,
                                enabled = uiState.currentPage < uiState.pageCount - 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
                            }
                        }
                    }

                    // Size Slider Card
                    if (uiState.isSignaturePlaced && uiState.signatureBitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.FormatSize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.sign_size_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${(uiState.signatureScale * 100).roundToInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { onSignatureScaleChanged((uiState.signatureScale - 0.1f).coerceIn(0.2f, 2.5f)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }
                                    Slider(
                                        value = uiState.signatureScale,
                                        onValueChange = onSignatureScaleChanged,
                                        valueRange = 0.2f..2.5f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    FilledTonalIconButton(
                                        onClick = { onSignatureScaleChanged((uiState.signatureScale + 0.1f).coerceIn(0.2f, 2.5f)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Add / Change Signature
                    FilledTonalButton(
                        onClick = onAddSignature,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isSignaturePlaced)
                                stringResource(R.string.sign_change_signature)
                            else
                                stringResource(R.string.sign_add_signature),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Save Document
                    Button(
                        onClick = onSaveDocument,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = uiState.isSignaturePlaced && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sign_save_document),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // ────────────────────────────────────────────────
            // PORTRAIT (DİKEY) LAYOUT
            // ────────────────────────────────────────────────
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PDF Viewer with exact aspect ratio sizing — NO CROPPING!
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    val pageAspect = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
                    val containerAspect = containerWidth / containerHeight

                    val (pageDisplayWidth, pageDisplayHeight) = if (pageAspect > containerAspect) {
                        containerWidth to (containerWidth / pageAspect)
                    } else {
                        (containerHeight * pageAspect) to containerHeight
                    }

                    // PDF Page Box — EXACT aspect ratio, ZERO cropping!
                    val memoizedPageBitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() }
                    Box(
                        modifier = Modifier
                            .size(pageDisplayWidth, pageDisplayHeight)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .background(Color.White)
                    ) {
                        Image(
                            bitmap = memoizedPageBitmap,
                            contentDescription = "PDF Page ${uiState.currentPage + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )

                        if (uiState.isSignaturePlaced && uiState.signatureBitmap != null) {
                            SignatureOverlay(
                                signatureBitmap = uiState.signatureBitmap,
                                pageWidthDp = pageDisplayWidth,
                                pageHeightDp = pageDisplayHeight,
                                normOffsetX = uiState.normOffsetX,
                                normOffsetY = uiState.normOffsetY,
                                scale = uiState.signatureScale,
                                onNormalizedPositionChanged = onSignaturePositionChanged,
                                onScaleChanged = onSignatureScaleChanged,
                                onDelete = onRemoveSignature
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Size Slider
                if (uiState.isSignaturePlaced && uiState.signatureBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.sign_size_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FilledTonalIconButton(
                                onClick = { onSignatureScaleChanged((uiState.signatureScale - 0.1f).coerceIn(0.2f, 2.5f)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            Slider(
                                value = uiState.signatureScale,
                                onValueChange = onSignatureScaleChanged,
                                valueRange = 0.2f..2.5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            FilledTonalIconButton(
                                onClick = { onSignatureScaleChanged((uiState.signatureScale + 0.1f).coerceIn(0.2f, 2.5f)) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${(uiState.signatureScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Page Navigation
                if (uiState.pageCount > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousPage,
                            enabled = uiState.currentPage > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
                        }
                        Text(
                            text = "${uiState.currentPage + 1} / ${uiState.pageCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = onNextPage,
                            enabled = uiState.currentPage < uiState.pageCount - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onAddSignature,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isSignaturePlaced)
                                stringResource(R.string.sign_change_signature)
                            else
                                stringResource(R.string.sign_add_signature),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onSaveDocument,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = uiState.isSignaturePlaced && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.sign_save_document),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        // ────────────────────────────────────────────────
        // EMPTY STATE: Prominent Clickable "Select PDF" Card
        // ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.6f else 0.95f)
                    .clickable { onSelectPdf() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.sign_select_pdf_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.sign_select_pdf_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onSelectPdf,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sign_select_pdf_btn),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
