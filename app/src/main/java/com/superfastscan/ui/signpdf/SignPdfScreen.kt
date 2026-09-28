package com.superfastscan.ui.signpdf

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.superfastscan.ui.tools.OpenPdfContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import java.io.File

/**
 * Main screen for the Sign PDF tool.
 *
 * When PDF signing is complete:
 * 1. Automatically opens the newly signed PDF in system viewer.
 * 2. Navigates back to the Home screen with a success banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    onNavigateBack: () -> Unit,
    onSignedComplete: () -> Unit = {},
    viewModel: SignPdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // File picker for selecting a PDF document
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = OpenPdfContract()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadPdf(it) }
    }

    // Auto open PDF and navigate to home upon successful save
    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete && uiState.savedFilePath != null) {
            val path = uiState.savedFilePath!!
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
            onSignedComplete()
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val hasSelectedFile = uiState.pdfUri != null || uiState.saveComplete
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
                        text = stringResource(R.string.tool_sign_pdf),
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
                    // Open PDF button
                    IconButton(
                        onClick = {
                            pdfPickerLauncher.launch(Unit)
                        }
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = stringResource(R.string.sign_select_pdf)
                        )
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
            SignYourselfTab(
                uiState = uiState,
                onSelectPdf = { pdfPickerLauncher.launch(Unit) },
                onAddSignature = { viewModel.showSignaturePad() },
                onSignaturePositionChanged = { x, y ->
                    viewModel.updateNormalizedPosition(x, y)
                },
                onSignatureScaleChanged = { scale ->
                    viewModel.updateSignatureScale(scale)
                },
                onRemoveSignature = { viewModel.removeSignature() },
                onPreviousPage = { viewModel.goToPage(uiState.currentPage - 1) },
                onNextPage = { viewModel.goToPage(uiState.currentPage + 1) },
                onSaveDocument = { viewModel.saveSignedPdf() }
            )
        }

        // Signature Pad Dialog
        if (uiState.showSignaturePad) {
            SignaturePadDialog(
                onDismiss = { viewModel.hideSignaturePad() },
                onSignatureSaved = { bitmap -> viewModel.setSignature(bitmap) }
            )
        }
    }
}
