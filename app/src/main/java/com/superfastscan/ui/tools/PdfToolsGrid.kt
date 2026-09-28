package com.superfastscan.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superfastscan.R
import com.superfastscan.ui.components.ToolCard

/**
 * Displays a 3-column grid of all PDF tools with a section header.
 * Designed to be placed inside a scrollable container on the Home Screen.
 *
 * @param onToolClick Callback invoked with the selected [PdfTool]
 * @param modifier Modifier for the root layout
 */
@Composable
fun PdfToolsGrid(
    onToolClick: (PdfTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = stringResource(R.string.pdf_tools_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
        )

        // 3-column tool grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = PdfTool.entries.toList(),
                key = { it.toolId }
            ) { tool ->
                ToolCard(
                    title = stringResource(tool.titleResId),
                    icon = tool.icon,
                    iconTint = tool.iconTint,
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}
