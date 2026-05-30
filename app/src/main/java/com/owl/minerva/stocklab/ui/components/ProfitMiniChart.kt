package com.owl.minerva.stocklab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun ProfitMiniChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val chartColor = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            if (values.size < 2) return@Canvas

            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            val range = (maxValue - minValue).takeIf { it != 0f } ?: 1f
            val xStep = size.width / (values.lastIndex)
            val points = values.mapIndexed { index, value ->
                val x = index * xStep
                val y = size.height - ((value - minValue) / range * size.height)
                Offset(x, y)
            }

            for (index in 0 until points.lastIndex) {
                drawLine(
                    color = chartColor,
                    start = points[index],
                    end = points[index + 1],
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }

            points.forEach { point ->
                drawCircle(
                    color = chartColor,
                    radius = 5f,
                    center = point,
                )
            }
        }
    }
}
