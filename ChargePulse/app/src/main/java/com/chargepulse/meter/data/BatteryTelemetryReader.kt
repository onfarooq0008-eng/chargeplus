package com.chargepulse.meter.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.chargepulse.meter.model.BatteryHealthStatus
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.ChargingSource
import com.chargepulse.meter.model.MonitorSettings
import kotlin.math.abs

class BatteryTelemetryReader(private val context: Context) {

    private val appContext = context.applicationContext
    private val batteryManager =
        appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun readSnapshot(
        settings: MonitorSettings,
        stickyIntentOverride: Intent? = null
    ): BatteryTelemetry {
        val batteryIntent = stickyIntentOverride ?: appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val levelRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scaleRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val levelPercent = if (levelRaw >= 0 && scaleRaw > 0) {
            ((levelRaw * 100f) / scaleRaw.toFloat()).toInt().coerceIn(0, 100)
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                .takeIf { it in 0..100 } ?: 0
        }

        val status = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        val pluggedRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isPlugged = pluggedRaw != 0
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || (isPlugged && !isFull && status != BatteryManager.BATTERY_STATUS_NOT_CHARGING)

        val chargingSource = resolveChargingSource(pluggedRaw)

        val rawCurrentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val rawCurrentAvg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

        val currentNowMa = BatteryMetricsCalculator.normalizeMicroAmpsToMilliAmps(
            rawMicroAmps = rawCurrentNow,
            invertPolarity = settings.invertCurrentPolarity
        )
        val currentAvgMa = BatteryMetricsCalculator.normalizeMicroAmpsToMilliAmps(
            rawMicroAmps = rawCurrentAvg,
            invertPolarity = settings.invertCurrentPolarity
        )

        // Some OEMs keep CURRENT_NOW positive even when discharging; normalize sign using charging state if needed.
        val resolvedCurrentMa = when {
            currentNowMa != null -> normalizeDirection(currentNowMa, isPlugged, isCharging, settings.invertCurrentPolarity)
            currentAvgMa != null -> normalizeDirection(currentAvgMa, isPlugged, isCharging, settings.invertCurrentPolarity)
            else -> 0f
        }
        val resolvedAvgMa = when {
            currentAvgMa != null -> normalizeDirection(currentAvgMa, isPlugged, isCharging, settings.invertCurrentPolarity)
            else -> resolvedCurrentMa
        }

        val rawVoltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageV = BatteryMetricsCalculator.normalizeVoltageToVolts(rawVoltage)

        val watts = BatteryMetricsCalculator.computeWatts(
            voltageVolts = voltageV.toDouble(),
            currentMilliamps = resolvedCurrentMa.toDouble()
        )
        val signedWatts = BatteryMetricsCalculator.computeSignedWatts(
            voltageVolts = voltageV.toDouble(),
            signedCurrentMilliamps = resolvedCurrentMa.toDouble(),
            isCharging = isCharging
        )

        val speedTier = BatteryMetricsCalculator.classifySpeedTier(
            isPlugged = isPlugged,
            isFull = isFull,
            isCharging = isCharging,
            watts = watts,
            fastThresholdW = settings.fastChargeThresholdWatts,
            superFastThresholdW = settings.superFastChargeThresholdWatts
        )

        val tempTenthsC = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250) ?: 250
        val temperatureC = tempTenthsC / 10f
        val thermalStatus = BatteryMetricsCalculator.classifyThermalStatus(temperatureC)

        val healthRaw = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthStatus = resolveHealthStatus(healthRaw)

        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() } ?: "Li-ion"

        val chargeCounterRaw = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeCounterMah = if (chargeCounterRaw != Int.MIN_VALUE && chargeCounterRaw > 0) {
            chargeCounterRaw / 1000f
        } else {
            null
        }

        val estimatedCapacityMah = BatteryMetricsCalculator.estimateFullChargeCapacityMah(
            chargeCounterMah = chargeCounterMah,
            levelPercent = levelPercent
        )
        val estimatedHealthPercent = BatteryMetricsCalculator.computeEstimatedHealthPercent(
            estimatedFullMah = estimatedCapacityMah,
            designCapacityMah = settings.designCapacityMah
        )

        val hardwareSoh = readHardwareStateOfHealth()
        val cycleCount = readCycleCount(batteryIntent)

        val etaMs = if (isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                batteryManager.computeChargeTimeRemaining().takeIf { it > 0L }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val maxNegotiatedWatts = readNegotiatedChargerWatts(batteryIntent, isPlugged)
        val ratePerHour = BatteryMetricsCalculator.estimateChargingRatePercentPerHour(
            currentMa = resolvedCurrentMa,
            designCapacityMah = settings.designCapacityMah,
            isCharging = isCharging
        )

        return BatteryTelemetry(
            levelPercent = levelPercent,
            isCharging = isCharging,
            isPlugged = isPlugged,
            chargingSource = chargingSource,
            speedTier = speedTier,
            isFastCharging = speedTier.isFastCategory,
            currentMa = resolvedCurrentMa,
            averageCurrentMa = resolvedAvgMa,
            voltageV = voltageV,
            watts = watts,
            signedWatts = signedWatts,
            temperatureC = temperatureC,
            thermalStatus = thermalStatus,
            healthStatus = healthStatus,
            technology = technology,
            chargeCounterMah = chargeCounterMah,
            estimatedCapacityMah = estimatedCapacityMah,
            estimatedHealthPercent = estimatedHealthPercent,
            hardwareStateOfHealthPercent = hardwareSoh,
            cycleCount = cycleCount,
            estimatedTimeToFullMs = etaMs,
            maxNegotiatedWatts = maxNegotiatedWatts,
            chargingRatePercentPerHour = ratePerHour,
            currentSensorSupported = currentNowMa != null || currentAvgMa != null,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun normalizeDirection(
        milliamps: Float,
        isPlugged: Boolean,
        isCharging: Boolean,
        invertPolarity: Boolean
    ): Float {
        // When user explicitly flips polarity, honor raw flipped sign directly.
        if (invertPolarity) return milliamps
        // If device is clearly unplugged/discharging and kernel returned positive discharge mA, show negative draw.
        return if (!isPlugged && !isCharging && milliamps > 0f) {
            -milliamps
        } else {
            milliamps
        }
    }

    private fun resolveChargingSource(pluggedRaw: Int): ChargingSource {
        if (pluggedRaw == 0) return ChargingSource.UNPLUGGED
        if ((pluggedRaw and BatteryManager.BATTERY_PLUGGED_AC) != 0) return ChargingSource.AC
        if ((pluggedRaw and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0) return ChargingSource.WIRELESS
        if ((pluggedRaw and BatteryManager.BATTERY_PLUGGED_USB) != 0) return ChargingSource.USB
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (pluggedRaw and BatteryManager.BATTERY_PLUGGED_DOCK) != 0
        ) {
            return ChargingSource.DOCK
        }
        return ChargingSource.AC
    }

    private fun resolveHealthStatus(healthRaw: Int): BatteryHealthStatus = when (healthRaw) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthStatus.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthStatus.OVERHEAT
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthStatus.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthStatus.DEAD
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthStatus.COLD
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthStatus.FAILURE
        else -> BatteryHealthStatus.UNKNOWN
    }

    private fun readHardwareStateOfHealth(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return try {
            val soh = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH)
            if (soh in 1..100) soh else null
        } catch (_: Exception) {
            null
        }
    }

    private fun readCycleCount(batteryIntent: Intent?): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val cycles = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        return if (cycles >= 0) cycles else null
    }

    private fun readNegotiatedChargerWatts(batteryIntent: Intent?, isPlugged: Boolean): Double? {
        if (!isPlugged || batteryIntent == null) return null
        val maxCurrentUa = batteryIntent.getIntExtra("max_charging_current", -1)
        val maxVoltageUv = batteryIntent.getIntExtra("max_charging_voltage", -1)
        if (maxCurrentUa <= 0 || maxVoltageUv <= 0) return null
        val currentA = abs(maxCurrentUa) / 1_000_000.0
        val voltageV = maxVoltageUv / 1_000_000.0
        val watts = currentA * voltageV
        return if (watts > 0.5 && watts < 300.0) watts else null
    }

    @SuppressLint("PrivateApi")
    fun readDesignCapacityFromPowerProfile(): Int? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val instance = constructor.newInstance(appContext)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (method.invoke(instance) as? Double)?.toInt()
            capacity?.takeIf { it in 1500..15000 }
        } catch (_: Exception) {
            null
        }
    }
}
