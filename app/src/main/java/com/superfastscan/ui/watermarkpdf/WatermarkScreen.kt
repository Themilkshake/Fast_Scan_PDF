package com.superfastscan.ui.watermarkpdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.superfastscan.ui.tools.OpenPdfContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.domain.usecase.WatermarkPageScope
import com.superfastscan.domain.usecase.WatermarkType
import com.superfastscan.ui.compresspdf.CompressPdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val WATERMARK_COLORS = listOf(
    0xFFFF0000L to "Red",
    0xFF000000L to "Black",
    0xFF757575L to "Grey",
    0xFF1976D2L to "Blue",
    0xFF388E3CL to "Green",
    0xFFF57C00L to "Orange"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    onNavigateBack: () -> Unit,
    onWatermarkComplete: () -> Unit = {},
    viewModel: WatermarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // PDF file picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadPdf(uri)
        }
    }

    // Image watermark picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    // Save As / Export Document launcher
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri: Uri? ->
        if (destinationUri != null && uiState.watermarkResult != null) {
            val sourcePath = uiState.watermarkResult!!.outputPath
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val sourceUri = if (sourcePath.startsWith("content://")) {
                            Uri.parse(sourcePath)
                        } else {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                File(sourcePath)
                            )
                        }

                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.watermark_save_success))
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Save failed: ${e.message}")
                }
            }
        }
    }

    // Notify completion when watermark succeeds
    LaunchedEffect(uiState.isComplete, uiState.watermarkResult) {
        if (uiState.isComplete && uiState.watermarkResult != null) {
            onWatermarkComplete()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val hasSelectedFile = uiState.pdfUri != null || uiState.watermarkResult != null
    val onBackClicked = {
        if (hasSelectedFile) {
            viewModel.reset()
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = hasSelectedFile) {
        viewModel.reset()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_watermark),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.pdfUri != null) {
                        IconButton(
                            onClick = {
                                pdfPickerLauncher.launch(Unit)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = stringResource(R.string.watermark_change_pdf)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.pdfUri == null) {
                // ────────────────────────────────────────────
                // EMPTY STATE: Large PDF Selection Card
                // ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clickable {
                                pdfPickerLauncher.launch(Unit)
                            },
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
                                    imageVector = Icons.Default.BrandingWatermark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.watermark_select_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.watermark_select_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    pdfPickerLauncher.launch(Unit)
                                },
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
                                    text = stringResource(R.string.watermark_select_btn),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (uiState.isComplete && uiState.watermarkResult != null) {
                // ────────────────────────────────────────────
                // RESULT STATE: View, Share, Save As
                // ────────────────────────────────────────────
                val result = uiState.watermarkResult!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(R.string.watermark_result_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = result.outputFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Summary Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.compress_pages_count),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${result.pageCount}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Dosya Boyutu",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CompressPdfViewModel.formatFileSize(result.fileSizeBytes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Saved location info pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.watermark_saved_location),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── View PDF ──
                    Button(
                        onClick = { openPdfFile(context, result.outputPath) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.watermark_btn_view),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Share PDF ──
                    FilledTonalButton(
                        onClick = { sharePdfFile(context, result.outputPath) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.watermark_btn_share),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Save As / Export to Folder ──
                    OutlinedButton(
                        onClick = { saveAsLauncher.launch(result.outputFileName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.watermark_btn_save_as),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Watermark Another PDF ──
                    TextButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.watermark_btn_another),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // ────────────────────────────────────────────
                // INTERACTIVE LIVE PREVIEW & CONTROLS
                // ────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Area: Live Preview Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.firstPagePreview != null) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val maxHeight = maxHeight
                                val maxWidth = maxWidth
                                val aspect = uiState.previewAspectRatio

                                // Fit preview within available constraints
                                val previewHeight = if (maxWidth * aspect <= maxHeight) maxWidth * aspect else maxHeight
                                val previewWidth = previewHeight / aspect

                                Box(
                                    modifier = Modifier
                                        .size(previewWidth, previewHeight)
                                        .shadow(8.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                ) {
                                    // Background page render
                                    val previewBitmap = remember(uiState.firstPagePreview) {
                                        uiState.firstPagePreview?.asImageBitmap()
                                    }
                                    if (previewBitmap != null) {
                                        Image(
                                            bitmap = previewBitmap,
                                            contentDescription = "PDF Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    // Interactive Watermark Overlay
                                    WatermarkOverlay(
                                        config = uiState.config,
                                        containerWidthDp = previewWidth,
                                        containerHeightDp = previewHeight,
                                        onPositionChanged = { nx, ny ->
                                            viewModel.setNormalizedPosition(nx, ny)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    }

                    // Bottom Area: Tabbed Control Panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            PrimaryTabRow(
                                selectedTabIndex = uiState.selectedTab.ordinal,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = uiState.selectedTab == WatermarkTab.CONTENT,
                                    onClick = { viewModel.setTab(WatermarkTab.CONTENT) },
                                    text = { Text(stringResource(R.string.watermark_tab_content), fontWeight = FontWeight.Bold) },
                                    icon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = uiState.selectedTab == WatermarkTab.STYLE,
                                    onClick = { viewModel.setTab(WatermarkTab.STYLE) },
                                    text = { Text(stringResource(R.string.watermark_tab_style), fontWeight = FontWeight.Bold) },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                Tab(
                                    selected = uiState.selectedTab == WatermarkTab.PAGES,
                                    onClick = { viewModel.setTab(WatermarkTab.PAGES) },
                                    text = { Text(stringResource(R.string.watermark_tab_pages), fontWeight = FontWeight.Bold) },
                                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                when (uiState.selectedTab) {
                                    WatermarkTab.CONTENT -> {
                                        // ────────────────────────────────
                                        // CONTENT TAB
                                        // ────────────────────────────────
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Type Toggle Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = uiState.config.type == WatermarkType.TEXT,
                                                    onClick = { viewModel.setType(WatermarkType.TEXT) },
                                                    label = { Text(stringResource(R.string.watermark_type_text), fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                )
                                                FilterChip(
                                                    selected = uiState.config.type == WatermarkType.IMAGE,
                                                    onClick = {
                                                        viewModel.setType(WatermarkType.IMAGE)
                                                        if (uiState.config.imageBitmap == null) {
                                                            imagePickerLauncher.launch("image/*")
                                                        }
                                                    },
                                                    label = { Text(stringResource(R.string.watermark_type_image), fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (uiState.config.type == WatermarkType.TEXT) {
                                                // Quick Preset Chips (Örnek, Taslak, Gizli, Kopya, Acil)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(
                                                        stringResource(R.string.watermark_preset_confidential),
                                                        stringResource(R.string.watermark_preset_draft),
                                                        stringResource(R.string.watermark_preset_sample),
                                                        stringResource(R.string.watermark_preset_copy),
                                                        stringResource(R.string.watermark_preset_urgent)
                                                    ).forEach { preset ->
                                                        val isSelected = uiState.config.text.equals(preset, ignoreCase = true)
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected)
                                                                MaterialTheme.colorScheme.primaryContainer
                                                            else
                                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            modifier = Modifier.clickable { viewModel.setText(preset) }
                                                        ) {
                                                            Text(
                                                                text = preset,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected)
                                                                    MaterialTheme.colorScheme.primary
                                                                else
                                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Text Input
                                                OutlinedTextField(
                                                    value = uiState.config.text,
                                                    onValueChange = { viewModel.setText(it) },
                                                    label = { Text(stringResource(R.string.watermark_input_label)) },
                                                    placeholder = { Text(stringResource(R.string.watermark_input_hint)) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp)
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Color & Bold Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        WATERMARK_COLORS.forEach { (colorHex, _) ->
                                                            val isSelected = uiState.config.textColor == colorHex
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(colorHex))
                                                                    .border(
                                                                        BorderStroke(
                                                                            if (isSelected) 2.dp else 1.dp,
                                                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                                                        ),
                                                                        CircleShape
                                                                    )
                                                                    .clickable { viewModel.setTextColor(colorHex) },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = if (colorHex == 0xFF000000L) Color.White else Color.Black,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Bold toggle button
                                                    FilterChip(
                                                        selected = uiState.config.isBold,
                                                        onClick = { viewModel.setBold(!uiState.config.isBold) },
                                                        label = { Text(stringResource(R.string.watermark_bold_label)) },
                                                        leadingIcon = { Icon(Icons.Default.FormatBold, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                    )
                                                }
                                            } else {
                                                // Image Watermark selector (Company Logo)
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        if (uiState.config.imageBitmap != null) {
                                                            Image(
                                                                bitmap = uiState.config.imageBitmap!!.asImageBitmap(),
                                                                contentDescription = "Image preview",
                                                                modifier = Modifier
                                                                    .size(60.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color.LightGray),
                                                                contentScale = ContentScale.Fit
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { imagePickerLauncher.launch("image/*") },
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                if (uiState.config.imageBitmap != null)
                                                                    stringResource(R.string.watermark_change_image_btn)
                                                                else
                                                                    stringResource(R.string.watermark_select_image_btn)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Info,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(16.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                text = stringResource(R.string.watermark_image_logo_hint),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    WatermarkTab.STYLE -> {
                                        // ────────────────────────────────
                                        // STYLE & POSITION TAB
                                        // ────────────────────────────────
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Opacity Section (%20 - %40 ideal range)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.watermark_opacity_label),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.watermark_opacity_hint),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (uiState.config.opacity in 0.20f..0.40f)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = "${(uiState.config.opacity * 100).roundToInt()}%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (uiState.config.opacity in 0.20f..0.40f)
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Quick Opacity Chips (%20, %30, %40)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    0.20f to "20% (Hafif)",
                                                    0.30f to "30% (Önerilen)",
                                                    0.40f to "40% (Belirgin)"
                                                ).forEach { (valOp, label) ->
                                                    val isSelected = (uiState.config.opacity * 100).roundToInt() == (valOp * 100).roundToInt()
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.clickable { viewModel.setOpacity(valOp) }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected)
                                                                MaterialTheme.colorScheme.onPrimary
                                                            else
                                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Slider(
                                                value = uiState.config.opacity,
                                                onValueChange = { viewModel.setOpacity(it) },
                                                valueRange = 0.10f..0.80f,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Orientation & Rotation Presets (-45° Diagonal Recommended)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.watermark_rotation_label),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    listOf(
                                                        -45f to "-45° (Çapraz)",
                                                        0f to "0° (Yatay)",
                                                        90f to "90° (Dikey)",
                                                        45f to "45°"
                                                    ).forEach { (angle, label) ->
                                                        val isSelected = uiState.config.rotation.roundToInt() == angle.roundToInt()
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isSelected)
                                                                MaterialTheme.colorScheme.primary
                                                            else
                                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            modifier = Modifier.clickable { viewModel.setRotation(angle) }
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected)
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                else
                                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Slider(
                                                value = uiState.config.rotation,
                                                onValueChange = { viewModel.setRotation(it) },
                                                valueRange = -90f..90f,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Scale Slider & Center Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.watermark_scale_label),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                FilledTonalButton(
                                                    onClick = { viewModel.centerWatermark() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                                ) {
                                                    Icon(Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(stringResource(R.string.watermark_btn_center), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            Slider(
                                                value = uiState.config.scale,
                                                onValueChange = { viewModel.setScale(it) },
                                                valueRange = 0.4f..2.5f,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Background Layer Guarantee Badge
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Layers,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.watermark_layer_badge),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    WatermarkTab.PAGES -> {
                                        // ────────────────────────────────
                                        // PAGES SCOPE TAB
                                        // ────────────────────────────────
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "${stringResource(R.string.watermark_scope_title)} (${uiState.totalPages} sayfa)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Scope option chips
                                            listOf(
                                                WatermarkPageScope.ALL_PAGES to stringResource(R.string.watermark_scope_all),
                                                WatermarkPageScope.EXCLUDE_FIRST to stringResource(R.string.watermark_scope_exclude_cover),
                                                WatermarkPageScope.FIRST_PAGE_ONLY to stringResource(R.string.watermark_scope_first_only),
                                                WatermarkPageScope.CUSTOM to stringResource(R.string.watermark_scope_custom)
                                            ).forEach { (scope, title) ->
                                                val isSelected = uiState.config.pageScope == scope
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.setPageScope(scope) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected)
                                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                                else
                                                                    MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (scope == WatermarkPageScope.EXCLUDE_FIRST) {
                                                                Text(
                                                                    text = stringResource(R.string.watermark_scope_exclude_cover_desc),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                                )
                                                            }
                                                        }

                                                        if (isSelected) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (uiState.config.pageScope == WatermarkPageScope.CUSTOM) {
                                                OutlinedTextField(
                                                    value = uiState.config.customPages,
                                                    onValueChange = { viewModel.setCustomPages(it) },
                                                    label = { Text(stringResource(R.string.watermark_scope_custom)) },
                                                    placeholder = { Text(stringResource(R.string.watermark_custom_pages_hint)) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Button: Apply Watermark
                            Button(
                                onClick = { viewModel.applyWatermark() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = uiState.totalPages > 0 && !uiState.isProcessing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrandingWatermark,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.watermark_action_btn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ────────────────────────────────────────────
            // Progress Dialog
            // ────────────────────────────────────────────
            if (uiState.isProcessing) {
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.watermark_progress_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${stringResource(R.string.watermark_progress_desc)} ${uiState.progressCurrent} / ${uiState.progressTotal}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openPdfFile(context: Context, path: String) {
    try {
        val uri = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path)
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun sharePdfFile(context: Context, path: String) {
    try {
        val uri = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path)
            )
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
