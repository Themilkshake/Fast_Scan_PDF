package com.superfastscan.ui.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superfastscan.R
import com.superfastscan.domain.model.PurchaseUiState
import com.superfastscan.domain.model.SubscriptionPlan

private val GoldGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFA000),
        Color(0xFFFF8F00)
    )
)

private val CardBackground = Color(0xFF1E293B)
private val DeepBackground = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismissRequest: () -> Unit,
    plan: SubscriptionPlan,
    lifetimePlan: SubscriptionPlan,
    purchaseState: PurchaseUiState,
    isPremium: Boolean,
    onSubscribeClicked: (Activity, Boolean) -> Unit,
    onRestoreClicked: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLifetimeSelected by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DeepBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 3D Gold Premium Logo Header ──
            Box(
                modifier = Modifier
                    .size(115.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo_premium),
                    contentDescription = stringResource(R.string.premium_title),
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFB300).copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.premium_badge_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Subtitle
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.premium_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Plan Selection Segmented Control ──
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val halfWidth = maxWidth / 2
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (isLifetimeSelected) halfWidth else 0.dp,
                        label = "indicatorOffset"
                    )

                    // The sliding animated background pill
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .offset(x = indicatorOffset)
                            .width(halfWidth - 8.dp) // padding on both sides
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF3B82F6))
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monthly Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Removes ripple effect for cleaner slide
                                ) { isLifetimeSelected = false },
                            contentAlignment = Alignment.Center
                        ) {
                            val textColor by animateColorAsState(
                                targetValue = if (!isLifetimeSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                label = "textColor"
                            )
                            Text(
                                text = "Aylık",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Lifetime Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // Removes ripple effect for cleaner slide
                                ) { isLifetimeSelected = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val textColor by animateColorAsState(
                                targetValue = if (isLifetimeSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                label = "textColor"
                            )
                            Text(
                                text = "Sınırsız",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Core Benefits ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BenefitItem(
                    icon = Icons.Default.Block,
                    iconTint = Color(0xFFEF4444),
                    title = stringResource(R.string.premium_benefit_adfree_title),
                    description = stringResource(R.string.premium_benefit_adfree_desc)
                )

                BenefitItem(
                    icon = Icons.Default.Bolt,
                    iconTint = Color(0xFFEAB308),
                    title = stringResource(R.string.premium_benefit_speed_title),
                    description = stringResource(R.string.premium_benefit_speed_desc)
                )

                BenefitItem(
                    icon = Icons.Default.Loop,
                    iconTint = Color(0xFF3B82F6),
                    title = stringResource(R.string.premium_benefit_tools_title),
                    description = stringResource(R.string.premium_benefit_tools_desc)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Plan Selection Removed from here ──

            // ── Purchase / State Feedback ──
            if (purchaseState is PurchaseUiState.Error) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = purchaseState.message,
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (purchaseState is PurchaseUiState.Restored) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (purchaseState.hasActiveSubscription) Color(0xFF14532D) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = if (purchaseState.hasActiveSubscription)
                            stringResource(R.string.premium_restore_success)
                        else
                            stringResource(R.string.premium_restore_not_found),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Main CTA Button ──
            val isButtonLoading = purchaseState is PurchaseUiState.Loading
            val selectedPrice = if (isLifetimeSelected) lifetimePlan.formattedPrice else plan.formattedPrice

            Button(
                onClick = {
                    activity?.let { onSubscribeClicked(it, isLifetimeSelected) }
                },
                enabled = !isButtonLoading && !isPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    disabledContainerColor = if (isPremium) Color(0xFF10B981) else Color(0xFF475569)
                )
            ) {
                if (isButtonLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                } else if (isPremium) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.premium_status_active),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.premium_subscribe_btn, selectedPrice),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Restore Purchases Button ──
            TextButton(
                onClick = onRestoreClicked,
                enabled = !isButtonLoading
            ) {
                Text(
                    text = stringResource(R.string.premium_restore_btn),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Auto-renewal & Legal Disclosures ──
            Text(
                text = stringResource(R.string.premium_auto_renew_disclosure),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terms of Use & Privacy Policy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.premium_terms_of_use),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.Underline,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        openUrl(context, "https://policies.google.com/terms")
                    }
                )
                Text(
                    text = "  •  ",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
                Text(
                    text = stringResource(R.string.premium_privacy_policy),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.Underline,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        openUrl(context, "https://policies.google.com/privacy")
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
