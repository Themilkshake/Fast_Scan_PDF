package com.superfastscan.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.superfastscan.R

// ── Color palette ──
private val AccentBlue = Color(0xFF3B82F6)
private val AccentIndigo = Color(0xFF6366F1)
private val BarBackground = Color(0xFF0F172A) // Deep Navy
private val BarBorder = Color(0xFF334155)     // Subtle slate border
private val InactiveColor = Color(0xFF94A3B8) // Slate-400
private val ActiveIndicator = Color(0xFF3B82F6)

@Composable
fun BottomScanBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onScanClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHome = currentRoute == "com.superfastscan.navigation.Home" || currentRoute == null
    val isHistory = currentRoute == "com.superfastscan.navigation.SavedScans"

    val barBackground = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
    val barBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── Glassmorphism-style Background Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(barBackground)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barBorder,
                            barBorder.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home button
                BottomBarItem(
                    icon = Icons.Rounded.Home,
                    label = stringResource(R.string.btn_home),
                    isSelected = isHome,
                    onClick = onHomeClick
                )

                // Spacer for center FAB
                Spacer(modifier = Modifier.width(72.dp))

                // History button
                BottomBarItem(
                    icon = Icons.Rounded.History,
                    label = stringResource(R.string.btn_history),
                    isSelected = isHistory,
                    onClick = onHistoryClick
                )
            }
        }

        // ── Center Scan FAB — Gradient, elevated ──
        val scanInteractionSource = remember { MutableInteractionSource() }
        val isPressed by scanInteractionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1f,
            animationSpec = tween(120),
            label = "scanScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle outer ring
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(barBackground)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AccentBlue.copy(alpha = 0.4f),
                                AccentIndigo.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = CircleShape,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    interactionSource = scanInteractionSource
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AccentBlue, AccentIndigo)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.btn_scan),
                            modifier = Modifier.size(26.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    // Simple color animation — lightweight, no recomposition overhead
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "iconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "labelColor"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(3.dp))
        // Active dot indicator
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isSelected) ActiveIndicator else Color.Transparent
                )
        )
    }
}

