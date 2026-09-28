package com.superfastscan.ui.saved

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.ui.components.AdBanner
import com.superfastscan.ui.components.BANNER_SAVED_SCANS_AD_UNIT_ID
import com.superfastscan.ui.components.DocumentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScansScreen(
    onNavigateToExport: (Long) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SavedScansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var documentToDelete by remember { mutableStateOf<Long?>(null) }
    var deleteFromFiles by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.btn_saved_scans),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (!uiState.isPremium) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AdBanner(
                            adUnitId = BANNER_SAVED_SCANS_AD_UNIT_ID,
                            isPremium = uiState.isPremium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (uiState.isEmpty) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_saved_scans),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_saved_scans_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.documents,
                    key = { it.id }
                ) { document ->
                    DocumentCard(
                        document = document,
                        onClick = {
                            document.exportedFilePath?.let { path ->
                                try {
                                    val uri = if (path.startsWith("content://")) {
                                        android.net.Uri.parse(path)
                                    } else {
                                        androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            java.io.File(path)
                                        )
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, if (path.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Dosya açılamadı veya bulunamadı.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } ?: run {
                                android.widget.Toast.makeText(context, "Bu tarama henüz dışa aktarılmamış.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEdit = { onNavigateToEdit(document.id) },
                        onDelete = { documentToDelete = document.id }
                    )
                }
            }
        }
    }

    if (documentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                documentToDelete = null
                deleteFromFiles = false
            },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_confirmation_message))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteFromFiles = !deleteFromFiles }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = deleteFromFiles,
                            onCheckedChange = { deleteFromFiles = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.delete_from_files_option),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    documentToDelete?.let { viewModel.deleteDocument(it, deleteFromFiles) }
                    documentToDelete = null
                    deleteFromFiles = false
                }) {
                    Text(stringResource(R.string.delete_confirm_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    documentToDelete = null
                    deleteFromFiles = false
                }) {
                    Text(stringResource(R.string.delete_cancel_button))
                }
            }
        )
    }
}
