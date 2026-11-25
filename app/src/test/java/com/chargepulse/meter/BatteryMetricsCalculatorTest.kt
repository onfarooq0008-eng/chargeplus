package com.chargepulse.meter

import com.chargepulse.meter.data.BatteryMetricsCalculator
import com.chargepulse.meter.model.BatteryHealthStatus
import com.chargepulse.meter.model.ChargingSpeedTier
import com.chargepulse.meter.model.ThermalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryMetricsCalculatorTest {

    @Test
    fun `computeWatts returns accurate product of voltage and current`() {
        val watts = BatteryMetricsCalculator.computeWatts(voltageVolts = 5.0, currentMilliamps = 3000.0)
        assertEquals(15.0, watts, 0.001)
    }

    @Test
    fun `normalizeMicroAmpsToMilliAmps handles Int MIN_VALUE sentinel and polarity`() {
        assertNull(BatteryMetricsCalculator.normalizeMicroAmpsToMilliAmps(Int.MIN_VALUE, false))
        assertEquals(2500f, BatteryMetricsCalculator.normalizeMicroAmpsToMilliAmps(2_500_000, false)!!, 0.01f)
        assertEquals(-2500f, BatteryMetricsCalculator.normalizeMicroAmpsToMilliAmps(2_500_000, true)!!, 0.01f)
    }

    @Test
    fun `normalizeVoltageToVolts handles millivolts and legacy volts`() {
        assertEquals(4.35f, BatteryMetricsCalculator.normalizeVoltageToVolts(4350), 0.001f)
        assertEquals(5.0f, BatteryMetricsCalculator.normalizeVoltageToVolts(5), 0.001f)
        assertEquals(0f, BatteryMetricsCalculator.normalizeVoltageToVolts(0), 0.001f)
    }

    @Test
    fun `classifySpeedTier differentiates slow standard fast and super fast charging`() {
        assertEquals(
            ChargingSpeedTier.DISCHARGING,
            BatteryMetricsCalculator.classifySpeedTier(false, false, false, 3.0, 12f, 25f)
        )
        assertEquals(
            ChargingSpeedTier.FULL,
            BatteryMetricsCalculator.classifySpeedTier(true, true, false, 0.5, 12f, 25f)
        )
        assertEquals(
            ChargingSpeedTier.SLOW,
            BatteryMetricsCalculator.classifySpeedTier(true, false, true, 2.5, 12f, 25f)
        )
        assertEquals(
            ChargingSpeedTier.STANDARD,
            BatteryMetricsCalculator.classifySpeedTier(true, false, true, 9.0, 12f, 25f)
        )
        assertEquals(
            ChargingSpeedTier.FAST,
            BatteryMetricsCalculator.classifySpeedTier(true, false, true, 18.0, 12f, 25f)
        )
        assertEquals(
            ChargingSpeedTier.SUPER_FAST,
            BatteryMetricsCalculator.classifySpeedTier(true, false, true, 33.0, 12f, 25f)
        )
    }

    @Test
    fun `thermal and health capacity calculations produce expected ranges`() {
        assertEquals(ThermalStatus.COOL, BatteryMetricsCalculator.classifyThermalStatus(21.0f))
        assertEquals(ThermalStatus.OPTIMAL, BatteryMetricsCalculator.classifyThermalStatus(32.5f))
        assertEquals(ThermalStatus.WARM, BatteryMetricsCalculator.classifyThermalStatus(40.0f))
        assertEquals(ThermalStatus.HOT, BatteryMetricsCalculator.classifyThermalStatus(45.5f))

        val estFull = BatteryMetricsCalculator.estimateFullChargeCapacityMah(2250f, 50)
        assertEquals(4500f, estFull!!, 0.1f)

        val healthPct = BatteryMetricsCalculator.computeEstimatedHealthPercent(estFull, 5000)
        assertEquals(90, healthPct)

        assertNull(BatteryMetricsCalculator.estimateFullChargeCapacityMah(500f, 10))
    }

    @Test
    fun `formatting helpers format watts amps eta and health summary cleanly`() {
        assertEquals("27.4 W", BatteryMetricsCalculator.formatWatts(27.42))
        assertEquals("+3.20 A", BatteryMetricsCalculator.formatAmpsAndMilliAmps(3200f))
        assertEquals("-450 mA", BatteryMetricsCalculator.formatAmpsAndMilliAmps(-450f))
        assertEquals("~45 min left", BatteryMetricsCalculator.formatDurationEta(45 * 60_000L))
        assertEquals(
            "Health: Good • 33.0°C • 94% Cap",
            BatteryMetricsCalculator.healthLabelSummary(BatteryHealthStatus.GOOD, 33.0f, false, 94)
        )
    }
}
