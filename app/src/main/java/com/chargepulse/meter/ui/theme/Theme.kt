package com.chargepulse.meter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.chargepulse.meter.model.BatteryHealthStatus
import com.chargepulse.meter.model.ChargingSpeedTier
import com.chargepulse.meter.model.ThermalStatus

object ChargePulseColors {
    val BgDark = Color(0xFF0B1018)
    val SurfaceDark = Color(0xFF131B28)
    val SurfaceElevated = Color(0xFF1A2436)
    val BorderSubtle = Color(0xFF27344A)
    val EmeraldFast = Color(0xFF00E676)
    val CyanSuperFast = Color(0xFF00D2FF)
    val AmberStandard = Color(0xFFFFB300)
    val RedAlert = Color(0xFFFF5252)
    val TextPrimary = Color(0xFFF4F7FB)
    val TextMuted = Color(0xFF9FB0C7)
}

private val DarkPalette = darkColorScheme(
    primary = ChargePulseColors.EmeraldFast,
    onPrimary = Color(0xFF042212),
    secondary = ChargePulseColors.CyanSuperFast,
    onSecondary = Color(0xFF031F29),
    tertiary = ChargePulseColors.AmberStandard,
    background = ChargePulseColors.BgDark,
    onBackground = ChargePulseColors.TextPrimary,
    surface = ChargePulseColors.SurfaceDark,
    onSurface = ChargePulseColors.TextPrimary,
    surfaceVariant = ChargePulseColors.SurfaceElevated,
    onSurfaceVariant = ChargePulseColors.TextMuted,
    error = ChargePulseColors.RedAlert,
    outline = ChargePulseColors.BorderSubtle
)

fun speedAccentColor(tier: ChargingSpeedTier): Color = when (tier) {
    ChargingSpeedTier.SUPER_FAST -> ChargePulseColors.CyanSuperFast
    ChargingSpeedTier.FAST, ChargingSpeedTier.FULL -> ChargePulseColors.EmeraldFast
    ChargingSpeedTier.STANDARD, ChargingSpeedTier.SLOW -> ChargePulseColors.AmberStandard
    ChargingSpeedTier.NOT_CHARGING -> ChargePulseColors.TextMuted
    ChargingSpeedTier.DISCHARGING -> ChargePulseColors.CyanSuperFast
}

fun healthAccentColor(health: BatteryHealthStatus): Color =
    if (health.isHealthy) ChargePulseColors.EmeraldFast else ChargePulseColors.RedAlert

fun thermalAccentColor(thermal: ThermalStatus): Color = when (thermal) {
    ThermalStatus.COOL -> ChargePulseColors.CyanSuperFast
    ThermalStatus.OPTIMAL -> ChargePulseColors.EmeraldFast
    ThermalStatus.WARM -> ChargePulseColors.AmberStandard
    ThermalStatus.HOT -> ChargePulseColors.RedAlert
}

@Composable
fun ChargePulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkPalette,
        content = content
    )
}
