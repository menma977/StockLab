package com.owl.minerva.stocklab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.owl.minerva.stocklab.ui.theme.StockLabDanger
import com.owl.minerva.stocklab.ui.theme.StockLabSuccess

@Composable
fun ProfitBadge(percent: Int) {
    val badgeColor = when {
        percent > 0 -> StockLabSuccess
        percent < 0 -> StockLabDanger
        else -> MaterialTheme.colorScheme.primary
    }
    val percentText = when {
        percent > 0 -> "+$percent%"
        percent < 0 -> "$percent%"
        else -> "0%"
    }

    Text(
        text = percentText,
        modifier = Modifier
            .background(
                color = badgeColor,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = androidx.compose.ui.graphics.Color.White,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
    )
}
