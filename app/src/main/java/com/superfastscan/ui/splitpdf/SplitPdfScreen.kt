package com.superfastscan.ui.splitpdf

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.ui.splitpdf.components.ExtractOptionsBottomSheet
import com.superfastscan.ui.splitpdf.components.PageGridCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(
    onNavigateBack: () -> Unit,
    onSplitComplete: () -> Unit = {},
    viewModel: SplitPdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Single PDF file picker launcher
    val singlePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadPdf(uri)
        }
    }

    // Auto-open first extracted PDF and navigate back upon completion
    LaunchedEffect(uiState.extractComplete) {
        if (uiState.extractComplete && uiState.extractedFilePaths.isNotEmpty()) {
            val firstPath = uiState.extractedFilePaths.first()
            try {
                val uri = if (firstPath.startsWith("content://")) {
                    Uri.parse(firstPath)
                } else {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(firstPath)
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
            onSplitComplete()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val hasSelectedFile = uiState.pdfUri != null || uiState.extractComplete
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
                        text = stringResource(R.string.tool_split_pdf),
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
                                singlePdfPickerLauncher.launch(Unit)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = stringResource(R.string.split_change_pdf)
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
                // EMPTY STATE: Large Centered Select Document Card
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
                                singlePdfPickerLauncher.launch(Unit)
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
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.split_select_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.split_select_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    singlePdfPickerLauncher.launch(Unit)
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
                                    text = stringResource(R.string.split_select_btn),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // ────────────────────────────────────────────
                // POPULATED STATE: Dual Mode Tab Screen
                // ────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Primary Tabs: Split All vs. Select Specific Pages
                    PrimaryTabRow(
                        selectedTabIndex = if (uiState.selectedTab == SplitTab.SPLIT_ALL) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Tab(
                            selected = uiState.selectedTab == SplitTab.SPLIT_ALL,
                            onClick = { viewModel.setTab(SplitTab.SPLIT_ALL) },
                            text = {
                                Text(
                                    text = stringResource(R.string.split_tab_all),
                                    fontWeight = if (uiState.selectedTab == SplitTab.SPLIT_ALL) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == SplitTab.SELECT_PAGES,
                            onClick = { viewModel.setTab(SplitTab.SELECT_PAGES) },
                            text = {
                                Text(
                                    text = stringResource(R.string.split_tab_custom),
                                    fontWeight = if (uiState.selectedTab == SplitTab.SELECT_PAGES) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    when (uiState.selectedTab) {
                        SplitTab.SPLIT_ALL -> {
                            // ────────────────────────────────────────────
                            // TAB 1: Split Every Page
                            // ────────────────────────────────────────────
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Information Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = uiState.fileName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(R.string.split_all_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Visual Grid Preview of All Pages
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.pages,
                                        key = { it.pageIndex }
                                    ) { page ->
                                        PageGridCard(
                                            pageItem = page,
                                            isSelected = true,
                                            onToggle = { }
                                        )
                                    }
                                }

                                // Bottom Sticky Bar: Split All Button
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shadowElevation = 8.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.splitAllPages() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            enabled = uiState.totalPages > 0 && !uiState.isExtracting
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.split_all_btn, uiState.totalPages),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SplitTab.SELECT_PAGES -> {
                            // ────────────────────────────────────────────
                            // TAB 2: Select Specific Pages & Range
                            // ────────────────────────────────────────────
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Controls Container (Range input & Quick Filters)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        // Page Range Text Input Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = uiState.rangeInput,
                                                onValueChange = { viewModel.updateRangeInput(it) },
                                                modifier = Modifier.weight(1f),
                                                placeholder = {
                                                    Text(
                                                        text = stringResource(R.string.split_range_hint),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Numbers,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (uiState.rangeInput.isNotEmpty()) {
                                                        IconButton(onClick = { viewModel.updateRangeInput("") }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Clear,
                                                                contentDescription = "Clear",
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Text,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        focusManager.clearFocus()
                                                        viewModel.applyPageRange(uiState.rangeInput)
                                                    }
                                                )
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            FilledTonalButton(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    viewModel.applyPageRange(uiState.rangeInput)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(50.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.split_range_apply),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Range Error message
                                        AnimatedVisibility(
                                            visible = uiState.rangeError != null,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            uiState.rangeError?.let { err ->
                                                Text(
                                                    text = err,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Quick Filter Chips Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AssistChip(
                                                onClick = { viewModel.selectAll() },
                                                label = {
                                                    Text(
                                                        text = stringResource(R.string.split_select_all),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )

                                            AssistChip(
                                                onClick = { viewModel.selectOddPages() },
                                                label = {
                                                    Text(
                                                        text = stringResource(R.string.split_filter_odd),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            )

                                            AssistChip(
                                                onClick = { viewModel.selectEvenPages() },
                                                label = {
                                                    Text(
                                                        text = stringResource(R.string.split_filter_even),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            )

                                            if (uiState.selectedPages.isNotEmpty()) {
                                                AssistChip(
                                                    onClick = { viewModel.deselectAll() },
                                                    label = {
                                                        Text(
                                                            text = stringResource(R.string.split_deselect_all),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        leadingIconContentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Counter info
                                        Text(
                                            text = "${uiState.selectedCount} / ${uiState.totalPages} ${stringResource(R.string.split_selected_count)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Visual 3-Column Page Grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.pages,
                                        key = { it.pageIndex }
                                    ) { page ->
                                        val isSelected = uiState.selectedPages.contains(page.pageIndex)
                                        PageGridCard(
                                            pageItem = page,
                                            isSelected = isSelected,
                                            onToggle = { viewModel.togglePageSelection(page.pageIndex) }
                                        )
                                    }
                                }

                                // Bottom Action Bar: Extract Button
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shadowElevation = 8.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.showExtractOptions() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            enabled = uiState.selectedPages.isNotEmpty() && !uiState.isExtracting
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (uiState.selectedPages.isNotEmpty())
                                                    "${stringResource(R.string.split_extract_btn)} (${uiState.selectedCount})"
                                                else
                                                    stringResource(R.string.split_extract_btn),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ────────────────────────────────────────────
            // Extraction Options Bottom Sheet
            // ────────────────────────────────────────────
            if (uiState.showExtractOptions) {
                ExtractOptionsBottomSheet(
                    selectedCount = uiState.selectedCount,
                    onSelectMode = { mode -> viewModel.extractPages(mode) },
                    onDismiss = { viewModel.hideExtractOptions() }
                )
            }

            // ────────────────────────────────────────────
            // Loading Overlay (Generating Page Thumbnails)
            // ────────────────────────────────────────────
            if (uiState.isLoadingPages) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Text(
                                text = stringResource(R.string.split_loading_pages),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ────────────────────────────────────────────
            // Extraction Progress Dialog
            // ────────────────────────────────────────────
            if (uiState.isExtracting) {
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
                                text = stringResource(R.string.split_progress_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${stringResource(R.string.split_progress_desc)} ${uiState.extractProgressCurrent} / ${uiState.extractProgressTotal}",
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
