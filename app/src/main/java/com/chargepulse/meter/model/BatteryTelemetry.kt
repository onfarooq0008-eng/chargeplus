package com.chargepulse.meter.model

enum class ChargingSource(val displayLabel: String) {
    AC("AC Wall Adapter"),
    USB("USB Port"),
    WIRELESS("Wireless Charger"),
    DOCK("Charging Dock"),
    UNPLUGGED("Battery Power")
}

enum class ChargingSpeedTier(
    val label: String,
    val shortBadge: String,
    val isFastCategory: Boolean
) {
    DISCHARGING("Discharging", "STANDBY", false),
    NOT_CHARGING("Plugged (Idle)", "IDLE", false),
    FULL("Battery Full", "FULL", false),
    SLOW("Slow / Trickle", "SLOW CHARGING", false),
    STANDARD("Standard Charge", "CHARGING", false),
    FAST("Fast Charging", "FAST CHARGING", true),
    SUPER_FAST("Super Fast Charging", "SUPER FAST", true)
}

enum class BatteryHealthStatus(val label: String, val isHealthy: Boolean) {
    GOOD("Good", true),
    OVERHEAT("Overheat", false),
    OVER_VOLTAGE("Over Voltage", false),
    DEAD("Dead / Degraded", false),
    COLD("Too Cold", false),
    FAILURE("Hardware Failure", false),
    UNKNOWN("Unknown", true)
}

enum class ThermalStatus(val label: String) {
    COOL("Cool"),
    OPTIMAL("Optimal"),
    WARM("Warm"),
    HOT("Hot")
}

data class SamplePoint(
    val timestampMs: Long,
    val watts: Double,
    val currentMa: Float,
    val voltageV: Float
)

data class BatteryTelemetry(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val isPlugged: Boolean = false,
    val chargingSource: ChargingSource = ChargingSource.UNPLUGGED,
    val speedTier: ChargingSpeedTier = ChargingSpeedTier.DISCHARGING,
    val isFastCharging: Boolean = false,
    val currentMa: Float = 0f,
    val averageCurrentMa: Float = 0f,
    val voltageV: Float = 0f,
    val watts: Double = 0.0,
    val signedWatts: Double = 0.0,
    val temperatureC: Float = 0f,
    val thermalStatus: ThermalStatus = ThermalStatus.OPTIMAL,
    val healthStatus: BatteryHealthStatus = BatteryHealthStatus.GOOD,
    val technology: String = "Li-ion",
    val chargeCounterMah: Float? = null,
    val estimatedCapacityMah: Float? = null,
    val estimatedHealthPercent: Int? = null,
    val hardwareStateOfHealthPercent: Int? = null,
    val cycleCount: Int? = null,
    val estimatedTimeToFullMs: Long? = null,
    val maxNegotiatedWatts: Double? = null,
    val chargingRatePercentPerHour: Float? = null,
    val currentSensorSupported: Boolean = true,
    val timestampMs: Long = 0L
)

data class MonitorSettings(
    val autoShowOverlayOnConnect: Boolean = true,
    val overlayAutoDismissSeconds: Int = 15,
    val backgroundMonitorEnabled: Boolean = true,
    val invertCurrentPolarity: Boolean = false,
    val useFahrenheit: Boolean = false,
    val designCapacityMah: Int = 5000,
    val fastChargeThresholdWatts: Float = 12f,
    val superFastChargeThresholdWatts: Float = 25f,
    val samplingIntervalMs: Long = 1000L
)

data class SessionStats(
    val peakWatts: Double = 0.0,
    val peakCurrentMa: Float = 0f,
    val averageWatts: Double = 0.0,
    val minTempC: Float = 0f,
    val maxTempC: Float = 0f,
    val startLevel: Int? = null,
    val connectedAtMs: Long? = null,
    val sampleCount: Int = 0
)
