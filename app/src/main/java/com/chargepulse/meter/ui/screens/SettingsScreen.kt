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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chargepulse.meter.model.MonitorSettings
import com.chargepulse.meter.ui.HomeIntegrationState
import com.chargepulse.meter.ui.theme.ChargePulseColors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: MonitorSettings,
    homeState: HomeIntegrationState,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onToggleAutoOverlay: (Boolean) -> Unit,
    onSetOverlayDismissSeconds: (Int) -> Unit,
    onToggleInvertPolarity: (Boolean) -> Unit,
    onToggleFahrenheit: (Boolean) -> Unit,
    onSetDesignCapacityMah: (Int) -> Unit,
    onSetThresholds: (Float, Float) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onPinWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Home Screen & Charger-Connect Behavior
        item {
            SettingsCard(title = "Home Page & Charger Detection") {
                SettingSwitchRow(
                    title = "Background Charger Monitor",
                    subtitle = "Runs a low-power foreground monitor so widget & HUD react instantly when plugged in.",
                    checked = settings.backgroundMonitorEnabled,
                    onCheckedChange = onToggleBackgroundMonitor
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Auto-Show Home Page Overlay",
                    subtitle = if (homeState.hasOverlayPermission) {
                        "Overlay permission granted. Shows floating Watt & Amp card when charger connects."
                    } else {
                        "Requires Display Over Other Apps permission."
                    },
                    checked = settings.autoShowOverlayOnConnect,
                    onCheckedChange = onToggleAutoOverlay
                )

                if (!homeState.hasOverlayPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Display Over Other Apps Permission")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Overlay Auto-Hide Timer",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10 to "10s", 15 to "15s", 30 to "30s", 0 to "Stay").forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.overlayAutoDismissSeconds == sec,
                            onClick = { onSetOverlayDismissSeconds(sec) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onPinWidgetClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChargePulseColors.EmeraldFast,
                        contentColor = Color(0xFF042212)
                    )
                ) {
                    Text(
                        text = if (homeState.hasPinnedWidget) {
                            "Home Screen Widget Pinned (Add Another)"
                        } else {
                            "Pin Live Watt & Amp Widget to Home Screen"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Calibration & Thresholds
        item {
            SettingsCard(title = "Meter Calibration & Units") {
                SettingSwitchRow(
                    title = "Invert Current Polarity (+/-)",
                    subtitle = "Enable if your phone shows negative mA while charging.",
                    checked = settings.invertCurrentPolarity,
                    onCheckedChange = onToggleInvertPolarity
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Use Fahrenheit (°F)",
                    subtitle = "Display battery temperature in °F instead of °C.",
                    checked = settings.useFahrenheit,
                    onCheckedChange = onToggleFahrenheit
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Design Battery Capacity: ${settings.designCapacityMah} mAh",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.designCapacityMah.toFloat(),
                    onValueChange = { onSetDesignCapacityMah((it / 50f).roundToInt() * 50) },
                    valueRange = 2500f..7500f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fast Charging Threshold: ${settings.fastChargeThresholdWatts.roundToInt()} W",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.fastChargeThresholdWatts,
                    onValueChange = {
                        onSetThresholds(it.roundToInt().toFloat(), settings.superFastChargeThresholdWatts)
                    },
                    valueRange = 8f..25f
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Super Fast Threshold: ${settings.superFastChargeThresholdWatts.roundToInt()} W",
                    color = ChargePulseColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = settings.superFastChargeThresholdWatts,
                    onValueChange = {
                        onSetThresholds(settings.fastChargeThresholdWatts, it.roundToInt().toFloat())
                    },
                    valueRange = 18f..65f
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ChargePulseColors.SurfaceDark),
        border = BorderStroke(1.dp, ChargePulseColors.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = ChargePulseColors.CyanSuperFast,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = ChargePulseColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = ChargePulseColors.TextMuted,
                fontSize = 12.sp
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
