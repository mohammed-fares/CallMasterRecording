package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import kotlin.random.Random

@Composable
fun CallMasterWaveform(
    modifier: Modifier = Modifier,
    progress: Float = 0f, // 0.0 to 1.0
    barCount: Int = 36,
    activeColor: Color = RadiantGold,
    inactiveColor: Color = PetroleumGreen.copy(alpha = 0.4f),
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    seed: Int = 42
) {
    val barHeights = remember(seed, barCount) {
        val rand = Random(seed)
        List(barCount) {
            0.2f + rand.nextFloat() * 0.8f
        }
    }

    Canvas(modifier = modifier.height(36.dp).fillMaxWidth()) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidthPx = barWidth.toPx()
        val spacingPx = barSpacing.toPx()
        val step = barWidthPx + spacingPx

        val activeIndex = (progress.coerceIn(0f, 1f) * barCount).toInt()

        for (i in 0 until barCount) {
            val x = i * step
            if (x + barWidthPx > totalWidth) break

            val hFraction = barHeights.getOrElse(i) { 0.5f }
            val barHeightPx = canvasHeight * hFraction
            val y = (canvasHeight - barHeightPx) / 2f

            val color = if (i <= activeIndex) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )
        }
    }
}
