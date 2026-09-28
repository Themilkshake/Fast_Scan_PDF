package com.superfastscan.ui.settings

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.text.style.TextAlign
import com.superfastscan.BuildConfig
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.os.Build
import android.app.LocaleManager
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superfastscan.R
import com.superfastscan.domain.model.AppLanguage
import com.superfastscan.domain.model.AppThemeMode
import com.superfastscan.ui.paywall.PaywallBottomSheet
import com.superfastscan.widget.QuickScanWidgetProvider

private enum class SettingsSubPage {
    THEME,
    SCAN_EXPORT,
    WIDGET,
    LANGUAGE,
    ABOUT,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptionPlan by viewModel.subscriptionPlan.collectAsStateWithLifecycle()
    val lifetimePlan by viewModel.lifetimePlan.collectAsStateWithLifecycle()
    val purchaseState by viewModel.purchaseState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    var currentSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Intercept back button when inside a sub-page
    BackHandler(enabled = currentSubPage != null) {
        currentSubPage = null
    }

    val pageTitle = when (currentSubPage) {
        null -> stringResource(R.string.btn_settings)
        SettingsSubPage.THEME -> stringResource(R.string.settings_theme)
        SettingsSubPage.SCAN_EXPORT -> stringResource(R.string.settings_scan_export_section)
        SettingsSubPage.WIDGET -> stringResource(R.string.widget_section_title)
        SettingsSubPage.LANGUAGE -> stringResource(R.string.settings_language)
        SettingsSubPage.ABOUT -> stringResource(R.string.settings_about)
        SettingsSubPage.CONTACT -> stringResource(R.string.settings_contact_us)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSubPage != null) {
                                currentSubPage = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentSubPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SettingsSubPageTransition"
            ) { subPage ->
                when (subPage) {
                    null -> {
                        // ── Main Settings Menu ──
                        SettingsMainMenu(
                            uiState = uiState,
                            onOpenPaywall = { showPaywall = true },
                            onSelectSubPage = { currentSubPage = it }
                        )
                    }

                    SettingsSubPage.THEME -> {
                        ThemeSubPage(
                            selectedThemeMode = uiState.selectedThemeMode,
                            onSetThemeMode = { viewModel.setThemeMode(it) }
                        )
                    }

                    SettingsSubPage.SCAN_EXPORT -> {
                        ScanExportSubPage(
                            defaultFormat = uiState.defaultFormat,
                            onSetDefaultFormat = { viewModel.setDefaultFormat(it) }
                        )
                    }

                    SettingsSubPage.WIDGET -> {
                        WidgetSubPage(
                            isPremium = uiState.isPremium
                        )
                    }

                    SettingsSubPage.LANGUAGE -> {
                        LanguageSubPage(
                            selectedLanguage = uiState.selectedLanguage,
                            onSetLanguage = { viewModel.setLanguage(it) }
                        )
                    }

                    SettingsSubPage.ABOUT -> {
                        AboutSubPage(
                            onNavigateToContact = { currentSubPage = SettingsSubPage.CONTACT }
                        )
                    }

                    SettingsSubPage.CONTACT -> {
                        ContactSubPage()
                    }
                }
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
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Settings Menu
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsMainMenu(
    uiState: SettingsUiState,
    onOpenPaywall: () -> Unit,
    onSelectSubPage: (SettingsSubPage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Premium Card ──
        PremiumCard(
            isPremium = uiState.isPremium,
            onClick = onOpenPaywall
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Navigation Categories Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            // 1. Theme
            SettingsMenuItem(
                icon = Icons.Default.Palette,
                iconTint = Color(0xFF8B5CF6),
                title = stringResource(R.string.settings_theme),
                subtitle = when (uiState.selectedThemeMode) {
                    AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                },
                onClick = { onSelectSubPage(SettingsSubPage.THEME) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 2. Scan & Export
            SettingsMenuItem(
                icon = Icons.Default.Tune,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.settings_scan_export_section),
                subtitle = if (uiState.defaultFormat == "PDF") {
                    stringResource(R.string.format_pdf_title)
                } else {
                    stringResource(R.string.format_jpg_title)
                },
                onClick = { onSelectSubPage(SettingsSubPage.SCAN_EXPORT) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 2. Quick Scan Widget
            SettingsMenuItem(
                icon = Icons.Default.Widgets,
                iconTint = if (uiState.isPremium) Color(0xFFFFC107) else Color(0xFF3B82F6),
                title = stringResource(R.string.widget_section_title),
                subtitle = stringResource(R.string.widget_scan_now),
                onClick = { onSelectSubPage(SettingsSubPage.WIDGET) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 3. Language
            SettingsMenuItem(
                icon = Icons.Default.Language,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_language),
                subtitle = uiState.selectedLanguage.displayName,
                onClick = { onSelectSubPage(SettingsSubPage.LANGUAGE) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 4. About
            SettingsMenuItem(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.app_version),
                onClick = { onSelectSubPage(SettingsSubPage.ABOUT) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // 5. Contact Us (Bize Ulaşın)
            SettingsMenuItem(
                icon = Icons.Default.Email,
                iconTint = Color(0xFF10B981),
                title = stringResource(R.string.settings_contact_us),
                onClick = { onSelectSubPage(SettingsSubPage.CONTACT) }
            )
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-Pages
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanExportSubPage(
    defaultFormat: String,
    onSetDefaultFormat: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_scan_export_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            // Format: PDF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetDefaultFormat("PDF") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.format_pdf_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.format_pdf_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (defaultFormat == "PDF") {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Format: JPG
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetDefaultFormat("JPG") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.format_jpg_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.format_jpg_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (defaultFormat == "JPG") {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Save Location (Info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_default_location),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.settings_default_location_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetSubPage(
    isPremium: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.widget_section_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1-Block Realistic Widget Preview
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val logoRes = if (isPremium) R.drawable.widget_logo_premium else R.drawable.widget_logo_standard
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val success = QuickScanWidgetProvider.requestPinWidget(context, isPremium)
                        if (success) {
                            Toast.makeText(context, R.string.widget_pin_requested, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, R.string.widget_pin_not_supported, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.widget_add_to_home),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSubPage(
    selectedLanguage: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            AppLanguage.entries.forEachIndexed { index, language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetLanguage(language)
                            val localeList = LocaleListCompat.forLanguageTags(language.code)
                            AppCompatDelegate.setApplicationLocales(localeList)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val localeManager = context.getSystemService(LocaleManager::class.java)
                                localeManager?.applicationLocales = LocaleList.forLanguageTags(language.code)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedLanguage == language) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (index < AppLanguage.entries.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSubPage(
    onNavigateToContact: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.developer_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.developer_name),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Contact Us Shortcut Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToContact() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_contact_us),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contact Us Sub-page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContactSubPage() {
    val context = LocalContext.current

    val onSendEmail = {
        val subject = context.getString(R.string.contact_email_subject_default)
        val body = context.getString(
            R.string.contact_email_body_simple,
            "${Build.MANUFACTURER} ${Build.MODEL}",
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
            BuildConfig.VERSION_NAME
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:seydialiiclek@gmail.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("seydialiiclek@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_contact_us)))
        } catch (e: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Email", "seydialiiclek@gmail.com"))
            Toast.makeText(context, context.getString(R.string.contact_no_email_client), Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.contact_us_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.contact_us_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Direct Send Email Button
                Button(
                    onClick = onSendEmail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.contact_send_email_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSubPage(
    selectedThemeMode: AppThemeMode,
    onSetThemeMode: (AppThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            val options = listOf(
                Triple(AppThemeMode.SYSTEM, R.string.theme_system, R.string.theme_system_desc),
                Triple(AppThemeMode.LIGHT, R.string.theme_light, R.string.theme_light_desc),
                Triple(AppThemeMode.DARK, R.string.theme_dark, R.string.theme_dark_desc)
            )

            options.forEachIndexed { index, (mode, titleRes, descRes) ->
                val icon = when (mode) {
                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                    AppThemeMode.DARK -> Icons.Default.DarkMode
                }
                val iconTint = when (mode) {
                    AppThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
                    AppThemeMode.LIGHT -> Color(0xFFF59E0B)
                    AppThemeMode.DARK -> Color(0xFF8B5CF6)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetThemeMode(mode) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iconTint.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedThemeMode == mode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (index < options.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Premium Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumCard(
    isPremium: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) Color(0xFF14532D).copy(alpha = 0.35f) else Color(0xFF1E293B)
        ),
        border = BorderStroke(
            1.dp,
            if (isPremium) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFFFB300).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_premium),
                contentDescription = null,
                modifier = Modifier.size(46.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.premium_settings_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPremium) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isPremium) stringResource(R.string.home_badge_premium) else stringResource(R.string.home_badge_free),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPremium) Color(0xFF34D399) else Color(0xFFFFC107),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isPremium)
                        stringResource(R.string.premium_status_active)
                    else
                        stringResource(R.string.premium_settings_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPremium) Color(0xFFA7F3D0) else Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isPremium) Icons.Default.Check else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isPremium) Color(0xFF10B981) else Color(0xFFFFC107),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
