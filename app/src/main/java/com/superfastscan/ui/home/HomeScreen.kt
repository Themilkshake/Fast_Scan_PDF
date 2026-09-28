package com.superfastscan.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.ui.components.AdBanner
import com.superfastscan.ui.components.BANNER_HOME_AD_UNIT_ID
import com.superfastscan.ui.components.GooglePlayReviewDialog
import com.superfastscan.ui.components.InitialSetupDialog
import com.superfastscan.ui.components.ToolCard
import com.superfastscan.ui.paywall.PaywallBottomSheet
import com.superfastscan.ui.tools.PdfTool
import com.superfastscan.util.findActivity

private val AccentBlue = Color(0xFF3B82F6)

@Composable
fun HomeScreen(
    onNavigateToExport: (Long) -> Unit,
    onNavigateToSavedScans: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTool: (PdfTool) -> Unit,
    showSuccessMessage: Boolean = false,
    onSuccessMessageDismissed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionPlan by viewModel.subscriptionPlan.collectAsStateWithLifecycle()
    val lifetimePlan by viewModel.lifetimePlan.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    var isSuccessMessageVisible by remember(showSuccessMessage) { mutableStateOf(showSuccessMessage) }
    var showAllTools by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var showUnlimitedScanDialog by remember { mutableStateOf(false) }
    val showReviewDialog by viewModel.showReviewDialog.collectAsStateWithLifecycle()

    if (!uiState.isInitialSetupCompleted) {
        InitialSetupDialog(
            onConfirm = { selectedFormat ->
                viewModel.completeInitialSetup(selectedFormat)
            }
        )
    }

    if (showReviewDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = remember(context) { context.findActivity() }
        GooglePlayReviewDialog(
            isPremium = uiState.isPremium,
            onRateClick = {
                activity?.let { viewModel.launchReview(it) } ?: viewModel.dismissReviewDialog()
            },
            onDismiss = {
                viewModel.dismissReviewDialog()
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Premium Ambient Blue Space Background (Cached Drawing, Zero Per-Frame Cost) ──
        if (uiState.isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val width = size.width
                        val height = size.height

                        // Pre-compiled gradient shaders cached on layout/size change
                        val leftGradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8).copy(alpha = 0.35f),
                                Color(0xFF2563EB).copy(alpha = 0.18f),
                                Color(0xFF3B82F6).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = width * 0.22f
                        )

                        val rightGradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF3B82F6).copy(alpha = 0.04f),
                                Color(0xFF2563EB).copy(alpha = 0.18f),
                                Color(0xFF1D4ED8).copy(alpha = 0.35f)
                            ),
                            startX = width * 0.78f,
                            endX = width
                        )

                        val leftRadial = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.22f),
                                Color(0xFF1E40AF).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(0f, height * 0.28f),
                            radius = width * 0.32f
                        )

                        val rightRadial = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.22f),
                                Color(0xFF1E40AF).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(width, height * 0.58f),
                            radius = width * 0.32f
                        )

                        onDrawBehind {
                            drawRect(brush = leftGradient, size = size)
                            drawRect(brush = rightGradient, size = size)
                            drawCircle(brush = leftRadial, center = Offset(0f, height * 0.28f), radius = width * 0.32f)
                            drawCircle(brush = rightRadial, center = Offset(width, height * 0.58f), radius = width * 0.32f)
                        }
                    }
            )
        }

        // Tool cards logic - cached to prevent per-recomposition allocations
        val visibleTools = remember(showAllTools) {
            if (showAllTools) PdfTool.entries else PdfTool.entries.take(6)
        }

        // ── Main content: Scrollable grid of PDF Tools ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (uiState.isPremium) 96.dp else 170.dp // Extra space for Sticky Banner if not premium
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Large Logo / Welcome Section
            item(span = { GridItemSpan(3) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo with Transparent Background (Dynamic Gold for Premium)
                    val logoRes = if (uiState.isPremium) R.drawable.premium_surum else R.drawable.default_surum
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(180.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Spacer above PDF tools
            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Section header — spans all 3 columns
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pdf_tools_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (showAllTools) stringResource(R.string.hide_all) else stringResource(R.string.show_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showAllTools = !showAllTools }
                            .padding(8.dp)
                    )
                }
            }

            items(
                items = visibleTools,
                key = { it.toolId }
            ) { tool ->
                ToolCard(
                    title = stringResource(tool.titleResId),
                    icon = tool.icon,
                    iconTint = tool.iconTint,
                    onClick = { onNavigateToTool(tool) }
                )
            }
        }

        // ── Top Right Action Bar (Premium Badge + Settings) ──
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

        val (badgeBgColor, badgeBorderColor, badgeContentColor) = remember(isLight, uiState.isPremium) {
            val bg = if (isLight) {
                if (uiState.isPremium) Color(0xFFFEF3C7) else Color(0xFFFFFBEB)
            } else {
                if (uiState.isPremium) Color(0xFFFFB300).copy(alpha = 0.20f) else Color(0xFFFFB300).copy(alpha = 0.15f)
            }
            val border = if (isLight) {
                if (uiState.isPremium) Color(0xFFF59E0B) else Color(0xFFFBBF24)
            } else {
                if (uiState.isPremium) Color(0xFFFFD700) else Color(0xFFFFB300).copy(alpha = 0.5f)
            }
            val content = if (isLight) {
                if (uiState.isPremium) Color(0xFF92400E) else Color(0xFFB45309)
            } else {
                if (uiState.isPremium) Color(0xFFFFD700) else Color(0xFFFFC107)
            }
            Triple(bg, border, content)
        }

        // ── Top Bar (Left: Unlimited Scan Notice, Right: Premium Badge + Settings) ──
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol Üst: Sınırsız Tarama İbaresi + Yuvarlak Tıklanabilir Ünlem İşareti
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showUnlimitedScanDialog = true }
                    .padding(end = 6.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_unlimited_scan_notice),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Yuvarlak Tıklanabilir Ünlem İşareti (Tam merkezli Canvas çizimi)
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(14.dp)) {
                    val radius = size.minDimension / 2f
                    val strokeWidth = 1.dp.toPx()

                    // Daire arka plan dolgusu
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.12f),
                        radius = radius
                    )

                    // Daire dış çerçeve çizgisi
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.45f),
                        radius = radius - strokeWidth / 2f,
                        style = Stroke(width = strokeWidth)
                    )

                    // Ünlem gövdesi (tam dikey ve yatay simetrik merkezleme)
                    val stemWidth = 1.6.dp.toPx()
                    val stemTop = center.y - 3.2.dp.toPx()
                    val stemBottom = center.y + 0.8.dp.toPx()
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, stemTop),
                        end = Offset(center.x, stemBottom),
                        strokeWidth = stemWidth,
                        cap = StrokeCap.Round
                    )

                    // Ünlem alt noktası (tam merkez hizalı)
                    val dotCenterY = center.y + 3.2.dp.toPx()
                    val dotRadius = stemWidth / 2f
                    drawCircle(
                        color = primaryColor,
                        radius = dotRadius,
                        center = Offset(center.x, dotCenterY)
                    )
                }
            }

            // Sağ Üst: Premium Rozeti + Ayarlar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showPaywall = true },
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBgColor,
                    border = BorderStroke(1.dp, badgeBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (uiState.isPremium) 10.dp else 12.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isPremium) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = badgeContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (uiState.isPremium) {
                                stringResource(R.string.home_badge_premium)
                            } else {
                                stringResource(R.string.home_badge_free)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeContentColor
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.btn_settings),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Success Banner ──
        AnimatedVisibility(
            visible = isSuccessMessageVisible,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.export_success),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Sticky Bottom Banner Ad (Fixed on Home Screen) ──
        if (!uiState.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                AdBanner(
                    adUnitId = BANNER_HOME_AD_UNIT_ID,
                    isPremium = uiState.isPremium
                )
            }
        }
    }

    if (showPaywall) {
        PaywallBottomSheet(
            onDismissRequest = {
                showPaywall = false
                viewModel.resetPurchaseState()
            },
            plan = subscriptionPlan,
            lifetimePlan = lifetimePlan,
            purchaseState = purchaseState,
            isPremium = uiState.isPremium,
            onSubscribeClicked = { activity, isLifetime ->
                viewModel.launchPurchase(activity, isLifetime)
            },
            onRestoreClicked = {
                viewModel.restorePurchases()
            }
        )
    }

    if (showUnlimitedScanDialog) {
        AlertDialog(
            onDismissRequest = { showUnlimitedScanDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.home_unlimited_scan_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_unlimited_scan_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUnlimitedScanDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_ok),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    LaunchedEffect(isSuccessMessageVisible) {
        if (isSuccessMessageVisible) {
            kotlinx.coroutines.delay(3000)
            isSuccessMessageVisible = false
            onSuccessMessageDismissed()
        }
    }
}

