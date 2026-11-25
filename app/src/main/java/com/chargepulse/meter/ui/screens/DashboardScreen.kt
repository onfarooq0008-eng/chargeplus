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
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.chargepulse.meter.model.SamplePoint
import com.chargepulse.meter.model.SessionStats
import com.chargepulse.meter.ui.HomeIntegrationState
import com.chargepulse.meter.ui.components.LiveWaveformChart
import com.chargepulse.meter.ui.components.PowerGaugeCard
import com.chargepulse.meter.ui.theme.ChargePulseColors
import com.chargepulse.meter.ui.theme.healthAccentColor
import java.util.Locale

@Composable
fun DashboardScreen(
    telemetry: BatteryTelemetry,
    samples: List<SamplePoint>,
    sessionStats: SessionStats,
    settings: MonitorSettings,
    homeState: HomeIntegrationState,
    onPinWidgetClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onPreviewOverlayClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PowerGaugeCard(
                telemetry = telemetry,
                superFastThresholdW = settings.superFastChargeThresholdWatts
            )
        }

        if (!telemetry.currentSensorSupported) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceElevated),
                    border = BorderStroke(1.dp, ChargePulseColors.AmberStandard.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Note: This device or emulator did not expose a live current sensor (CURRENT_NOW). Real Android hardware reports live mA and Watts.",
                        color = ChargePulseColors.AmberStandard,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Home Page / Charger-Connected Display Setup Card
        item {
            HomePageIntegrationCard(
                homeState = homeState,
                onPinWidgetClick = onPinWidgetClick,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onPreviewOverlayClick = onPreviewOverlayClick,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }

        // Charging Speed & Session Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "CHARGING SPEED",
                    value = telemetry.speedTier.label,
                    subtitle = telemetry.chargingRatePercentPerHour?.let {
                        String.format(Locale.US, "+%.1f%%/hr", it)
                    } ?: "Standby",
                    accent = ChargePulseColors.EmeraldFast,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "TIME TO FULL",
                    value = if (telemetry.isCharging) {
                        BatteryMetricsCalculator.formatDurationEta(telemetry.estimatedTimeToFullMs)
                    } else if (telemetry.isPlugged) {
                        "Full / Idle"
                    } else {
                        "Unplugged"
                    },
                    subtitle = telemetry.maxNegotiatedWatts?.let {
                        "Max: ${BatteryMetricsCalculator.formatWatts(it)}"
                    } ?: "Avg: ${BatteryMetricsCalculator.formatMilliAmpsCompact(telemetry.averageCurrentMa)}",
                    accent = ChargePulseColors.CyanSuperFast,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "SESSION PEAK",
                    value = BatteryMetricsCalculator.formatWatts(sessionStats.peakWatts),
                    subtitle = "Peak ${BatteryMetricsCalculator.formatMilliAmpsCompact(sessionStats.peakCurrentMa)}",
                    accent = ChargePulseColors.AmberStandard,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "BATTERY HEALTH",
                    value = telemetry.healthStatus.label,
                    subtitle = BatteryMetricsCalculator.formatTemperature(
                        telemetry.temperatureC,
                        settings.useFahrenheit
                    ) + " • ${telemetry.thermalStatus.label}",
                    accent = healthAccentColor(telemetry.healthStatus),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            LiveWaveformChart(samples = samples)
        }
    }
}

@Composable
private fun HomePageIntegrationCard(
    homeState: HomeIntegrationState,
    onPinWidgetClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onPreviewOverlayClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
        border = BorderStroke(1.dp, ChargePulseColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = ChargePulseColors.EmeraldFast,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Android Home Page Display",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pin the live ChargePulse widget to your Android home page or enable the instant charger-connected floating meter.",
                color = ChargePulseColors.TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPinWidgetClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChargePulseColors.EmeraldFast,
                        contentColor = Color(0xFF042212)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (homeState.hasPinnedWidget) "Widget Active" else "Add Widget",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!homeState.hasOverlayPermission) {
                    OutlinedButton(
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ChargePulseColors.CyanSuperFast)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = ChargePulseColors.CyanSuperFast,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enable HUD",
                            color = ChargePulseColors.CyanSuperFast,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onPreviewOverlayClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ChargePulseColors.CyanSuperFast)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = ChargePulseColors.CyanSuperFast,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preview HUD",
                            color = ChargePulseColors.CyanSuperFast,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!homeState.hasNotificationPermission) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ChargePulseColors.AmberStandard)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = ChargePulseColors.AmberStandard,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Allow Notification for Live Charger Service",
                        color = ChargePulseColors.AmberStandard,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
        border = BorderStroke(1.dp, ChargePulseColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = ChargePulseColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = ChargePulseColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
