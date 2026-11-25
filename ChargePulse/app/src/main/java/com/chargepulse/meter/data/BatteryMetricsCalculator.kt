package com.chargepulse.meter.data

import com.chargepulse.meter.model.BatteryHealthStatus
import com.chargepulse.meter.model.ChargingSpeedTier
import com.chargepulse.meter.model.ThermalStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure telemetry calculation and formatting helpers isolated from Android framework APIs
 * so charging physics, speed tier classification, and health estimates can be unit tested directly.
 */
object BatteryMetricsCalculator {

    fun normalizeMicroAmpsToMilliAmps(
        rawMicroAmps: Int,
        invertPolarity: Boolean
    ): Float? {
        if (rawMicroAmps == Int.MIN_VALUE) return null
        val milliamps = rawMicroAmps / 1000f
        return if (invertPolarity) -milliamps else milliamps
    }

    fun normalizeVoltageToVolts(rawExtraVoltage: Int): Float {
        if (rawExtraVoltage <= 0) return 0f
        // Standard Android EXTRA_VOLTAGE is in millivolts (e.g., 4200 mV); guard rare legacy volts.
        val millivolts = if (rawExtraVoltage < 100) rawExtraVoltage * 1000 else rawExtraVoltage
        return millivolts / 1000f
    }

    fun computeWatts(voltageVolts: Double, currentMilliamps: Double): Double {
        if (voltageVolts <= 0.0) return 0.0
        return voltageVolts * (abs(currentMilliamps) / 1000.0)
    }

    fun computeSignedWatts(
        voltageVolts: Double,
        signedCurrentMilliamps: Double,
        isCharging: Boolean
    ): Double {
        val magnitude = computeWatts(voltageVolts, signedCurrentMilliamps)
        if (magnitude == 0.0) return 0.0
        return if (signedCurrentMilliamps > 0.0 || isCharging) magnitude else -magnitude
    }

    fun classifySpeedTier(
        isPlugged: Boolean,
        isFull: Boolean,
        isCharging: Boolean,
        watts: Double,
        fastThresholdW: Float,
        superFastThresholdW: Float
    ): ChargingSpeedTier {
        if (!isPlugged) return ChargingSpeedTier.DISCHARGING
        if (isFull) return ChargingSpeedTier.FULL
        if (!isCharging) return ChargingSpeedTier.NOT_CHARGING

        val safeFast = fastThresholdW.coerceAtLeast(6f).toDouble()
        val safeSuperFast = superFastThresholdW.coerceAtLeast(safeFast.toFloat() + 2f).toDouble()

        return when {
            watts >= safeSuperFast -> ChargingSpeedTier.SUPER_FAST
            watts >= safeFast -> ChargingSpeedTier.FAST
            watts >= 5.0 -> ChargingSpeedTier.STANDARD
            else -> ChargingSpeedTier.SLOW
        }
    }

    fun classifyThermalStatus(tempC: Float): ThermalStatus = when {
        tempC < 25f -> ThermalStatus.COOL
        tempC <= 38f -> ThermalStatus.OPTIMAL
        tempC <= 43f -> ThermalStatus.WARM
        else -> ThermalStatus.HOT
    }

    fun estimateFullChargeCapacityMah(
        chargeCounterMah: Float?,
        levelPercent: Int
    ): Float? {
        if (chargeCounterMah == null || chargeCounterMah <= 0f) return null
        if (levelPercent !in 15..100) return null
        return chargeCounterMah / (levelPercent / 100f)
    }

    fun computeEstimatedHealthPercent(
        estimatedFullMah: Float?,
        designCapacityMah: Int
    ): Int? {
        if (estimatedFullMah == null || designCapacityMah <= 0) return null
        val pct = ((estimatedFullMah / designCapacityMah.toFloat()) * 100f).roundToInt()
        return pct.coerceIn(1, 100)
    }

    fun estimateChargingRatePercentPerHour(
        currentMa: Float,
        designCapacityMah: Int,
        isCharging: Boolean
    ): Float? {
        if (!isCharging || designCapacityMah <= 0) return null
        val positiveMa = abs(currentMa)
        if (positiveMa < 10f) return null
        return (positiveMa / designCapacityMah.toFloat()) * 100f
    }

    fun formatWatts(watts: Double): String =
        String.format(Locale.US, "%.1f W", watts)

    fun formatAmpsAndMilliAmps(currentMa: Float, includeSign: Boolean = true): String {
        val roundedMa = currentMa.roundToInt()
        val sign = when {
            !includeSign -> ""
            roundedMa > 0 -> "+"
            else -> ""
        }
        return if (abs(roundedMa) >= 1000) {
            String.format(Locale.US, "%s%.2f A", sign, currentMa / 1000f)
        } else {
            String.format(Locale.US, "%s%d mA", sign, roundedMa)
        }
    }

    fun formatMilliAmpsCompact(currentMa: Float): String {
        val rounded = currentMa.roundToInt()
        val prefix = if (rounded > 0) "+" else ""
        return String.format(Locale.US, "%s%,d mA", prefix, rounded)
    }

    fun formatVoltage(voltageV: Float): String =
        String.format(Locale.US, "%.2f V", voltageV)

    fun formatTemperature(tempC: Float, useFahrenheit: Boolean): String {
        return if (useFahrenheit) {
            val tempF = (tempC * 9f / 5f) + 32f
            String.format(Locale.US, "%.1f°F", tempF)
        } else {
            String.format(Locale.US, "%.1f°C", tempC)
        }
    }

    fun formatDurationEta(millis: Long?): String {
        if (millis == null || millis <= 0L) return "Calculating..."
        val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val mins = totalMinutes % 60L
        return if (hours > 0) {
            "~${hours}h ${mins}m left"
        } else {
            "~${mins} min left"
        }
    }

    fun healthLabelSummary(
        health: BatteryHealthStatus,
        tempC: Float,
        useFahrenheit: Boolean,
        healthPercent: Int?
    ): String {
        val tempStr = formatTemperature(tempC, useFahrenheit)
        val pctSuffix = healthPercent?.let { " • $it% Cap" } ?: ""
        return "Health: ${health.label} • $tempStr$pctSuffix"
    }
}
