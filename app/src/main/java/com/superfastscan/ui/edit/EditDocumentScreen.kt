package com.superfastscan.ui.edit

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.superfastscan.R
import com.superfastscan.domain.model.FilterType
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDocumentScreen(
    documentId: Long,
    onSaveComplete: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EditDocumentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    BackHandler {
        showExitConfirmDialog = true
    }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.pages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_pages_remaining),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onNavigateBack() }) {
                    Text(stringResource(R.string.back))
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentPageIndex,
        pageCount = { uiState.pages.size }
    )
    val thumbnailListState = rememberLazyListState()

    // Sync pager -> viewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentPage(page)
        }
    }

    // Scroll thumbnail list when page changes
    LaunchedEffect(uiState.currentPageIndex) {
        thumbnailListState.animateScrollToItem(uiState.currentPageIndex)
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(stringResource(R.string.exit_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.exit_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.exit_confirm_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(stringResource(R.string.exit_cancel_button), fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.file_name), fontWeight = FontWeight.Bold) },
            text = {
                val fileExtension = if (uiState.selectedFormat.equals("JPG", ignoreCase = true)) ".jpg" else ".pdf"
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text(stringResource(R.string.file_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    suffix = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = fileExtension,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            val cleanTitle = renameText.trim()
                                .removeSuffix(".pdf")
                                .removeSuffix(".PDF")
                                .removeSuffix(".jpg")
                                .removeSuffix(".JPG")
                                .removeSuffix(".jpeg")
                                .removeSuffix(".JPEG")
                            viewModel.updateDocumentTitle(cleanTitle)
                        }
                        showRenameDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.delete_cancel_button))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val fileExtension = if (uiState.selectedFormat.equals("JPG", ignoreCase = true)) ".jpg" else ".pdf"
                    val displayTitle = uiState.documentTitle
                        .removeSuffix(".pdf")
                        .removeSuffix(".PDF")
                        .removeSuffix(".jpg")
                        .removeSuffix(".JPG")
                        .removeSuffix(".jpeg")
                        .removeSuffix(".JPEG")
                        .ifBlank { stringResource(R.string.untitled_document) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                renameText = displayTitle
                                showRenameDialog = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = fileExtension,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Delete page button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = uiState.pages.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Filter chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentPage = uiState.pages.getOrNull(uiState.currentPageIndex)
                    val currentFilter = currentPage?.filterType ?: FilterType.ORIGINAL

                    FilterType.entries.forEach { filter ->
                        val filterName = when (filter) {
                            FilterType.ORIGINAL -> stringResource(R.string.filter_original)
                            FilterType.GRAYSCALE -> stringResource(R.string.filter_grayscale)
                            FilterType.BLACK_WHITE -> stringResource(R.string.filter_black_white)
                            FilterType.ENHANCED -> stringResource(R.string.filter_enhanced)
                        }
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { viewModel.applyFilter(documentId, filter) },
                            label = {
                                Text(
                                    filterName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Unified Bottom action bar
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotation controls
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = { viewModel.rotatePage(documentId, -90f) }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = stringResource(R.string.rotate_left)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { viewModel.rotatePage(documentId, 90f) }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = stringResource(R.string.rotate_right)
                                )
                            }
                        }

                        // Save Document Button (Merged & Auto-open)
                        Button(
                            onClick = {
                                viewModel.saveAndExport(documentId) { exportedPath ->
                                    // Automatically open document in external viewer
                                    exportedPath?.let { path ->
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
                                            val mimeType = if (path.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.error_open_file),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    onSaveComplete()
                                }
                            },
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_save_document),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Page indicator and format status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.page_indicator, uiState.currentPageIndex + 1, uiState.pages.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${stringResource(R.string.format_default_badge)}: ${uiState.selectedFormat}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Main page preview (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { pageIndex ->
                val page = uiState.pages[pageIndex]
                val imagePath = page.editedImagePath ?: page.originalImagePath

                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val colorMatrix = remember(page.filterType) {
                            when (page.filterType) {
                                FilterType.GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }
                                FilterType.BLACK_WHITE -> ColorMatrix(floatArrayOf(
                                    1.5f, 1.5f, 1.5f, 0f, -200f,
                                    1.5f, 1.5f, 1.5f, 0f, -200f,
                                    1.5f, 1.5f, 1.5f, 0f, -200f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                                FilterType.ENHANCED -> ColorMatrix(floatArrayOf(
                                    1.3f, 0f, 0f, 0f, 10f,
                                    0f, 1.3f, 0f, 0f, 10f,
                                    0f, 0f, 1.3f, 0f, 10f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                                FilterType.ORIGINAL -> null
                            }
                        }
                        AsyncImage(
                            model = if (imagePath.startsWith("content://")) {
                                Uri.parse(imagePath)
                            } else {
                                File(imagePath)
                            },
                            contentDescription = "Page ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .graphicsLayer {
                                    rotationZ = page.rotation
                                }
                                .padding(8.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thumbnail navigation bar
            LazyRow(
                state = thumbnailListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.pages,
                    key = { _, page -> page.id }
                ) { index, page ->
                    val isSelected = index == uiState.currentPageIndex
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        animationSpec = tween(200),
                        label = "thumbnailBorder"
                    )
                    val imagePath = page.editedImagePath ?: page.originalImagePath

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    ) {
                        AsyncImage(
                            model = if (imagePath.startsWith("content://")) {
                                Uri.parse(imagePath)
                            } else {
                                File(imagePath)
                            },
                            contentDescription = "Thumbnail ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Page number badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_page_title)) },
            text = { Text(stringResource(R.string.delete_page_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePage(documentId)
                    showDeleteDialog = false
                }) {
                    Text(
                        stringResource(R.string.delete_confirm_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete_cancel_button))
                }
            }
        )
    }
}
