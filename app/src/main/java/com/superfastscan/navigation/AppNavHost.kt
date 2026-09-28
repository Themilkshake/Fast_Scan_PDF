package com.superfastscan.navigation


import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.fillMaxSize
import com.superfastscan.ui.compresspdf.CompressPdfScreen
import com.superfastscan.ui.edit.EditDocumentScreen
import com.superfastscan.ui.home.HomeScreen
import com.superfastscan.ui.mergepdf.MergePdfScreen
import com.superfastscan.ui.organizepdf.OrganizePdfScreen
import com.superfastscan.ui.rotatepdf.RotatePdfScreen
import com.superfastscan.ui.saved.SavedScansScreen
import com.superfastscan.ui.settings.SettingsScreen
import com.superfastscan.ui.signpdf.SignPdfScreen
import com.superfastscan.ui.splitpdf.SplitPdfScreen
import com.superfastscan.ui.tools.PdfTool
import com.superfastscan.ui.officetopdf.OfficeDocType
import com.superfastscan.ui.officetopdf.OfficeToPdfScreen
import com.superfastscan.ui.jpgtopdf.JpgToPdfScreen
import com.superfastscan.ui.pdftojpg.PdfToJpgScreen
import com.superfastscan.ui.pdftooffice.PdfToOfficeScreen
import com.superfastscan.ui.pdftooffice.PdfToOfficeType
import com.superfastscan.ui.ocr.OcrScreen
import com.superfastscan.ui.watermarkpdf.WatermarkScreen
import com.superfastscan.ads.AdManager
import com.superfastscan.util.findActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.superfastscan.R

private const val TRANSITION_DURATION = 250


@Composable
fun AppNavHost(
    adManager: AdManager,
    navController: NavHostController = rememberNavController(),
    autoStartScan: Boolean = false,
    onAutoScanConsumed: () -> Unit = {}
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route
    val showBottomBar = currentRoute?.contains("Home") == true || currentRoute?.contains("SavedScans") == true

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = androidx.compose.runtime.remember(context) { context.findActivity() }
    val homeViewModel: com.superfastscan.ui.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    val scannerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.let {
                val pdfUri = it.pdf?.uri?.toString()
                val imageUris = it.pages?.map { page -> page.imageUri.toString() } ?: emptyList()
                homeViewModel.saveScanResult(pdfUri, imageUris) { docId ->
                    navController.navigate(EditDocument(docId))
                }
            }
        }
    }

    val onStartScan: () -> Unit = {
        val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(100)
            .setResultFormats(
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            .build()
        
        val client = com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
        activity?.let { act ->
            client.getStartScanIntent(act)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { e ->
                    android.widget.Toast.makeText(act, "Failed to start scanner: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Auto-start scan if triggered by widget or external shortcut
    androidx.compose.runtime.LaunchedEffect(autoStartScan) {
        if (autoStartScan) {
            onStartScan()
            onAutoScanConsumed()
        }
    }

    // Called when a tool completes and shows an ad (stays on result screen)
    val onToolActionComplete: () -> Unit = {
        activity?.let { adManager.trackToolActionAndShowAdIfNeeded(it) }
    }

    // Called when a tool completes and returns to Home (Merge, Split, Sign, Organize)
    val onToolCompleteAndPopHome: () -> Unit = {
        activity?.let { adManager.trackToolActionAndShowAdIfNeeded(it) }
        navController.previousBackStackEntry?.savedStateHandle?.set("showSuccessMessage", true)
        navController.popBackStack(Home, inclusive = false)
    }

    val onActionComplete: () -> Unit = onToolCompleteAndPopHome

    val onScanSaveComplete: () -> Unit = {
        homeViewModel.onScanCompleted(activity, adManager)
        navController.previousBackStackEntry?.savedStateHandle?.set("showSuccessMessage", true)
        navController.popBackStack(Home, inclusive = false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val showAdOverlay by adManager.showAdOverlay.collectAsStateWithLifecycle()
        if (showAdOverlay) {
            Dialog(
                onDismissRequest = { /* Cannot be dismissed manually */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .background(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.ad_loading), 
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Home,
            enterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_DURATION))
            }
        ) {
            composable<Home> { backStackEntry ->
                val showSuccessMessage = backStackEntry.savedStateHandle.get<Boolean>("showSuccessMessage") ?: false
                HomeScreen(
                    onNavigateToExport = { documentId ->
                        navController.navigate(EditDocument(documentId))
                    },
                    onNavigateToSavedScans = { navController.navigate(SavedScans) },
                    onNavigateToSettings = { navController.navigate(Settings) },
                    onNavigateToTool = { tool ->
                        navController.navigate(PdfToolRoute(tool.toolId))
                    },
                    showSuccessMessage = showSuccessMessage,
                    onSuccessMessageDismissed = {
                        backStackEntry.savedStateHandle.remove<Boolean>("showSuccessMessage")
                    }
                )
            }

            composable<Export> { backStackEntry ->
                val route: Export = backStackEntry.toRoute()
                EditDocumentScreen(
                    documentId = route.documentId,
                    onSaveComplete = {
                        onScanSaveComplete()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<SavedScans> {
                SavedScansScreen(
                    onNavigateToExport = { documentId ->
                        navController.navigate(EditDocument(documentId))
                    },
                    onNavigateToEdit = { documentId ->
                        navController.navigate(EditDocument(documentId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<EditDocument> { backStackEntry ->
                val route: EditDocument = backStackEntry.toRoute()
                EditDocumentScreen(
                    documentId = route.documentId,
                    onSaveComplete = {
                        onScanSaveComplete()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<PdfToolRoute> { backStackEntry ->
                val route: PdfToolRoute = backStackEntry.toRoute()
                val tool = PdfTool.fromId(route.toolId)
                when (tool) {
                    PdfTool.MERGE_PDF -> {
                        MergePdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onMergeComplete = {
                                onToolCompleteAndPopHome()
                            }
                        )
                    }
                    PdfTool.SPLIT_PDF -> {
                        SplitPdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSplitComplete = {
                                onToolCompleteAndPopHome()
                            }
                        )
                    }
                    PdfTool.COMPRESS_PDF -> {
                        CompressPdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCompressComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.SIGN_PDF -> {
                        SignPdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSignedComplete = {
                                onToolCompleteAndPopHome()
                            }
                        )
                    }
                    PdfTool.WATERMARK -> {
                        WatermarkScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onWatermarkComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.ROTATE_PDF -> {
                        RotatePdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onRotateComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.ORGANIZE_PDF -> {
                        OrganizePdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOrganizeComplete = {
                                onToolCompleteAndPopHome()
                            }
                        )
                    }
                    PdfTool.WORD_TO_PDF -> {
                        OfficeToPdfScreen(
                            docType = OfficeDocType.WORD,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.PPT_TO_PDF -> {
                        OfficeToPdfScreen(
                            docType = OfficeDocType.PPT,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.EXCEL_TO_PDF -> {
                        OfficeToPdfScreen(
                            docType = OfficeDocType.EXCEL,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.JPG_TO_PDF -> {
                        JpgToPdfScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.PDF_TO_JPG -> {
                        PdfToJpgScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.PDF_TO_WORD -> {
                        PdfToOfficeScreen(
                            type = PdfToOfficeType.WORD,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.PDF_TO_EXCEL -> {
                        PdfToOfficeScreen(
                            type = PdfToOfficeType.EXCEL,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.PDF_TO_PPT -> {
                        PdfToOfficeScreen(
                            type = PdfToOfficeType.PPT,
                            onNavigateBack = { navController.popBackStack() },
                            onConversionComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    PdfTool.OCR -> {
                        OcrScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOcrComplete = {
                                onToolActionComplete()
                            }
                        )
                    }
                    null -> {}
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showBottomBar,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
        ) {
            com.superfastscan.ui.components.BottomScanBar(
                currentRoute = currentRoute,
                onHomeClick = {
                    if (currentRoute?.contains("Home") != true) navController.navigate(Home) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onScanClick = onStartScan,
                onHistoryClick = {
                    if (currentRoute?.contains("SavedScans") != true) navController.navigate(SavedScans) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
