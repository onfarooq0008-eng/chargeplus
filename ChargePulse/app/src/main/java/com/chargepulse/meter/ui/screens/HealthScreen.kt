package com.chargepulse.meter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargepulse.meter.data.BatteryMetricsCalculator
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.MonitorSettings
import com.chargepulse.meter.model.SessionStats
import com.chargepulse.meter.ui.theme.ChargePulseColors
import com.chargepulse.meter.ui.theme.healthAccentColor
import com.chargepulse.meter.ui.theme.thermalAccentColor
import kotlin.math.roundToInt

@Composable
fun HealthScreen(
    telemetry: BatteryTelemetry,
    sessionStats: SessionStats,
    settings: MonitorSettings,
    modifier: Modifier = Modifier
) {
    val healthColor = healthAccentColor(telemetry.healthStatus)
    val thermalColor = thermalAccentColor(telemetry.thermalStatus)
    val displayHealthPct = telemetry.hardwareStateOfHealthPercent ?: telemetry.estimatedHealthPercent

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Health Verdict Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
                border = BorderStroke(1.dp, healthColor.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = healthColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Health",
                                color = ChargePulseColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = healthColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = telemetry.healthStatus.label.uppercase(),
                                color = healthColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (displayHealthPct != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = if (telemetry.hardwareStateOfHealthPercent != null) {
                                        "Hardware State of Health"
                                    } else {
                                        "Estimated Capacity Health"
                                    },
                                    color = ChargePulseColors.TextMuted,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$displayHealthPct%",
                                    color = ChargePulseColors.TextPrimary,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = telemetry.technology,
                                color = ChargePulseColors.CyanSuperFast,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { displayHealthPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = healthColor,
                            trackColor = ChargePulseColors.SurfaceElevated
                        )
                    } else {
                        Text(
                            text = "System Status: ${telemetry.healthStatus.label} (${telemetry.technology})",
                            color = ChargePulseColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Charge counter telemetry is not exposed by this kernel or emulator; real devices display capacity health % once battery level is >= 15%.",
                            color = ChargePulseColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Thermal Diagnostics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = thermalColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Thermal Diagnostics",
                                color = ChargePulseColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = telemetry.thermalStatus.label,
                            color = thermalColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DiagnosticColumn(
                            label = "CURRENT TEMP",
                            value = BatteryMetricsCalculator.formatTemperature(
                                telemetry.temperatureC,
                                settings.useFahrenheit
                            ),
                            valueColor = thermalColor
                        )
                        DiagnosticColumn(
                            label = "SESSION MIN",
                            value = BatteryMetricsCalculator.formatTemperature(
                                sessionStats.minTempC.takeIf { sessionStats.sampleCount > 0 }
                                    ?: telemetry.temperatureC,
                                settings.useFahrenheit
                            ),
                            valueColor = ChargePulseColors.TextPrimary
                        )
                        DiagnosticColumn(
                            label = "SESSION MAX",
                            value = BatteryMetricsCalculator.formatTemperature(
                                sessionStats.maxTempC.takeIf { sessionStats.sampleCount > 0 }
                                    ?: telemetry.temperatureC,
                                settings.useFahrenheit
                            ),
                            valueColor = ChargePulseColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Capacity & Cycle Count Diagnostics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
                border = BorderStroke(1.dp, ChargePulseColors.BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = ChargePulseColors.CyanSuperFast
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cell Capacity & Cycles",
                            color = ChargePulseColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DiagnosticRow(
                        "Design Capacity (Configured)",
                        "${settings.designCapacityMah} mAh"
                    )
                    DiagnosticRow(
                        "Estimated Full Capacity",
                        telemetry.estimatedCapacityMah?.let { "${it.roundToInt()} mAh" } ?: "Unavailable"
                    )
                    DiagnosticRow(
                        "Remaining Charge Counter",
                        telemetry.chargeCounterMah?.let { "${it.roundToInt()} mAh" } ?: "Unavailable"
                    )
                    DiagnosticRow(
                        "Charge Cycle Count",
                        telemetry.cycleCount?.toString() ?: "Not exposed (Android 14+ HAL)"
                    )
                    DiagnosticRow(
                        "Cell Chemistry",
                        telemetry.technology
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fast Charging & Longevity Tips",
                        color = ChargePulseColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Keep battery temperature under 40°C while fast charging to reduce lithium plating.\n" +
                            "• High wattage peaks normally taper down above 70–80% battery level by charger design.\n" +
                            "• Calibrate your device Design Capacity in Settings for accurate estimated health %.",
                        color = ChargePulseColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            color = ChargePulseColors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = ChargePulseColors.TextMuted, fontSize = 13.sp)
        Text(
            text = value,
            color = ChargePulseColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
