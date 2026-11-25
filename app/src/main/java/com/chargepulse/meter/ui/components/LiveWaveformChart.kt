package com.chargepulse.meter.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargepulse.meter.model.SamplePoint
import com.chargepulse.meter.ui.theme.ChargePulseColors
import kotlin.math.abs
import kotlin.math.max

@Composable
fun LiveWaveformChart(
    samples: List<SamplePoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
        border = BorderStroke(1.dp, ChargePulseColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Power & Ampere Curve",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot("Watts", ChargePulseColors.EmeraldFast)
                    LegendDot("Amps", ChargePulseColors.CyanSuperFast)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(ChargePulseColors.BgDark, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (samples.size < 2) {
                    Text(
                        text = "Collecting live telemetry samples...",
                        color = ChargePulseColors.TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Subtle horizontal reference grid lines
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                            val y = height * ratio
                            drawLine(
                                color = ChargePulseColors.BorderSubtle,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }

                        val maxWatts = max(
                            samples.maxOf { it.watts }.toFloat(),
                            10f
                        )
                        val maxAmps = max(
                            samples.maxOf { abs(it.currentMa) / 1000f },
                            2f
                        )

                        val wattPath = Path()
                        val ampPath = Path()
                        val stepX = width / (samples.size - 1).coerceAtLeast(1)

                        samples.forEachIndexed { index, point ->
                            val x = index * stepX
                            val wattNorm = (point.watts.toFloat() / maxWatts).coerceIn(0f, 1f)
                            val ampNorm = ((abs(point.currentMa) / 1000f) / maxAmps).coerceIn(0f, 1f)

                            val wy = height - (wattNorm * (height - 8.dp.toPx()) + 4.dp.toPx())
                            val ay = height - (ampNorm * (height - 8.dp.toPx()) + 4.dp.toPx())

                            if (index == 0) {
                                wattPath.moveTo(x, wy)
                                ampPath.moveTo(x, ay)
                            } else {
                                wattPath.lineTo(x, wy)
                                ampPath.lineTo(x, ay)
                            }
                        }

                        drawPath(
                            path = ampPath,
                            color = ChargePulseColors.CyanSuperFast,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawPath(
                            path = wattPath,
                            color = ChargePulseColors.EmeraldFast,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = ChargePulseColors.TextMuted,
            fontSize = 12.sp
        )
    }
}
