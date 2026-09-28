package com.superfastscan.ui.organizepdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.superfastscan.ui.tools.OpenPdfContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.domain.model.OrganizePageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizePdfScreen(
    onNavigateBack: () -> Unit,
    onOrganizeComplete: () -> Unit = {},
    viewModel: OrganizePdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var showAddPageSheet by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<OrganizePageItem?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    // PDF Pickers
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            viewModel.loadPdf(it)
        }
    }

    val appendPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            viewModel.onAppendPdf(it)
        }
    }

    // Save As (SAF Create Document)
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        destUri?.let { uri ->
            uiState.organizeResult?.let { result ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val srcStream = if (result.outputUri != null) {
                            context.contentResolver.openInputStream(result.outputUri)
                        } else {
                            File(result.outputPath).inputStream()
                        }
                        srcStream?.use { input ->
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(context.getString(R.string.organize_save_success))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.organizeResult) {
        if (uiState.organizeResult != null) {
            showExportSuccessDialog = true
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val hasSelectedFile = uiState.pages.isNotEmpty() || uiState.organizeResult != null
    val onBackClicked = {
        if (hasSelectedFile) {
            viewModel.resetState()
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = hasSelectedFile) {
        viewModel.resetState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.tool_organize_pdf),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.pages.isNotEmpty()) {
                            Text(
                                text = "${uiState.pages.size} ${stringResource(R.string.pages)} • ${uiState.fileName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.pages.isNotEmpty()) {
                        // Change PDF Button
                        IconButton(onClick = { pdfPickerLauncher.launch(Unit) }) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = stringResource(R.string.organize_change_pdf),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Save Button
                        FilledTonalButton(
                            onClick = { viewModel.saveOrganizedPdf() },
                            enabled = !uiState.isSaving && uiState.pages.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.organize_save_btn),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.pages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPageSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.organize_add_page_btn), fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoadingPages -> {
                    // Loading page previews
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = stringResource(R.string.split_loading_pages),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.pages.isEmpty() -> {
                    // Empty state: Select PDF Prompt
                    OrganizeEmptyState(
                        onSelectPdf = { pdfPickerLauncher.launch(Unit) }
                    )
                }

                else -> {
                    // ────────────────────────────────────────────────
                    // Interactive Drag-and-Drop Reorderable Grid View
                    // ────────────────────────────────────────────────
                    OrganizeDragDropGrid(
                        pages = uiState.pages,
                        onReorder = { from, to ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onReorder(from, to)
                        },
                        onDeleteClick = { pageToDelete = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Add Page Bottom Sheet
    // ────────────────────────────────────────────────────────────
    if (showAddPageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddPageSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.organize_add_page_btn),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Option 1: Add Blank Page
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddPageSheet = false
                            viewModel.onAddBlankPage()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.organize_add_blank_page),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.organize_add_blank_page_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Append Another PDF
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddPageSheet = false
                            appendPdfPickerLauncher.launch(Unit)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.organize_append_pdf),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.organize_append_pdf_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Delete Confirmation Dialog
    // ────────────────────────────────────────────────────────────
    pageToDelete?.let { item ->
        val pageNumber = uiState.pages.indexOf(item) + 1
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.organize_delete_page_title)) },
            text = { Text(stringResource(R.string.organize_delete_page_msg, pageNumber)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeletePage(item.id)
                        pageToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDelete = null }) {
                    Text(stringResource(R.string.delete_cancel_button))
                }
            }
        )
    }

    // ────────────────────────────────────────────────────────────
    // Saving Progress Dialog
    // ────────────────────────────────────────────────────────────
    if (uiState.isSaving) {
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
                        text = stringResource(R.string.organize_saving_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.organize_saving_desc)} ${uiState.progressCurrent} / ${uiState.progressTotal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Export Success Dialog
    // ────────────────────────────────────────────────────────────
    if (showExportSuccessDialog && uiState.organizeResult != null) {
        val result = uiState.organizeResult!!
        AlertDialog(
            onDismissRequest = {
                showExportSuccessDialog = false
                onOrganizeComplete()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.organize_result_title),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.outputFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${result.pageCount} ${stringResource(R.string.pages)} • ${formatFileSize(result.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.organize_saved_location),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openPdfFile(context, result.outputPath) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.organize_btn_view), fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { sharePdfFile(context, result.outputPath) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.organize_btn_share), fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { saveAsLauncher.launch(result.outputFileName) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.organize_btn_save_as), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportSuccessDialog = false
                        onOrganizeComplete()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportSuccessDialog = false
                        viewModel.resetState()
                    }
                ) {
                    Text(stringResource(R.string.organize_btn_another))
                }
            }
        )
    }
}

// ────────────────────────────────────────────────────────────────
// Empty State: Select Initial PDF
// ────────────────────────────────────────────────────────────────

@Composable
private fun OrganizeEmptyState(
    onSelectPdf: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = stringResource(R.string.organize_select_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.organize_select_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelectPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.organize_select_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// True Drag-and-Drop Reorderable Grid View
// ────────────────────────────────────────────────────────────────

@Composable
private fun OrganizeDragDropGrid(
    pages: List<OrganizePageItem>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDeleteClick: (OrganizePageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dropTargetIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Store bounding boxes of each cell in grid coordinate space
    val itemBounds = remember { mutableMapOf<Int, Rect>() }

    Column(modifier = modifier) {
        // Drag Hint Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.organize_drag_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = pages,
                key = { _, item -> item.id }
            ) { index, item ->
                val isDragging = draggingIndex == index
                val isDropTarget = dropTargetIndex == index && draggingIndex != -1 && draggingIndex != index

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            itemBounds[index] = coordinates.boundsInParent()
                        }
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.08f
                                scaleY = 1.08f
                                shadowElevation = 16.dp.toPx()
                            }
                        }
                        .pointerInput(pages.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dropTargetIndex = index
                                    dragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    val from = draggingIndex
                                    val to = dropTargetIndex
                                    draggingIndex = -1
                                    dropTargetIndex = -1
                                    dragOffset = Offset.Zero
                                    if (from != -1 && to != -1 && from != to) {
                                        onReorder(from, to)
                                    }
                                },
                                onDragCancel = {
                                    draggingIndex = -1
                                    dropTargetIndex = -1
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount

                                    // Find candidate target slot under center of dragged card
                                    val currentRect = itemBounds[draggingIndex]
                                    if (currentRect != null) {
                                        val centerOfDragged = currentRect.center + dragOffset
                                        val targetEntry = itemBounds.entries.find { (_, rect) ->
                                            rect.contains(centerOfDragged)
                                        }
                                        if (targetEntry != null) {
                                            dropTargetIndex = targetEntry.key
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    OrganizePageCard(
                        pageItem = item,
                        pageNumber = index + 1,
                        isDragging = isDragging,
                        isDropTarget = isDropTarget,
                        onDelete = { onDeleteClick(item) }
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Individual Page Thumbnail Card with Delete & Badges
// ────────────────────────────────────────────────────────────────

@Composable
private fun OrganizePageCard(
    pageItem: OrganizePageItem,
    pageNumber: Int,
    isDragging: Boolean,
    isDropTarget: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            else if (isDropTarget) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 12.dp else 2.dp
        ),
        border = BorderStroke(
            width = if (isDragging || isDropTarget) 2.dp else 1.dp,
            color = if (isDragging) MaterialTheme.colorScheme.primary
            else if (isDropTarget) MaterialTheme.colorScheme.secondary
            else Color.LightGray.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail Image
            val memoizedBitmap = remember(pageItem.thumbnail) {
                pageItem.thumbnail?.asImageBitmap()
            }

            if (memoizedBitmap != null) {
                Image(
                    bitmap = memoizedBitmap,
                    contentDescription = "Page $pageNumber",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.organize_blank_page_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Top-Left: Page Number Badge (Fixed during drag, updates only upon drop)
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp, topStart = 10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "#$pageNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            // Top-Right: Delete Corner Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clickable { onDelete() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(3.dp)
                )
            }

            // Bottom: Blank Page Badge or Source Note if Blank
            if (pageItem.isBlankPage) {
                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    color = Color(0xFF616161).copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.organize_blank_page_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Helpers: File Utilities
// ────────────────────────────────────────────────────────────────

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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
        else -> "$bytes B"
    }
}
