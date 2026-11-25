package com.chargepulse.meter.data

import android.content.Context
import android.content.Intent
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.SamplePoint
import com.chargepulse.meter.model.SessionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class BatteryMonitorRepository(
    context: Context,
    val settingsStore: SettingsStore
) {
    private val reader = BatteryTelemetryReader(context)

    init {
        reader.readDesignCapacityFromPowerProfile()?.let { detected ->
            settingsStore.setInitialDetectedCapacityIfUnset(detected)
        }
    }

    private val _telemetry = MutableStateFlow(reader.readSnapshot(settingsStore.current))
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    private val _samples = MutableStateFlow<List<SamplePoint>>(emptyList())
    val samples: StateFlow<List<SamplePoint>> = _samples.asStateFlow()

    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    private var wasPlugged: Boolean = _telemetry.value.isPlugged
    private var cumulativeWatts: Double = 0.0

    fun refreshNow(stickyIntentOverride: Intent? = null): BatteryTelemetry {
        val snapshot = reader.readSnapshot(settingsStore.current, stickyIntentOverride)
        _telemetry.value = snapshot

        val nextSamples = (_samples.value + SamplePoint(
            timestampMs = snapshot.timestampMs,
            watts = snapshot.watts,
            currentMa = snapshot.currentMa,
            voltageV = snapshot.voltageV
        )).takeLast(MAX_SAMPLES)
        _samples.value = nextSamples

        updateSessionStats(snapshot)
        return snapshot
    }

    private fun updateSessionStats(snapshot: BatteryTelemetry) {
        val current = _sessionStats.value
        val plugChanged = snapshot.isPlugged != wasPlugged
        wasPlugged = snapshot.isPlugged

        if (plugChanged) {
            cumulativeWatts = snapshot.watts
            _sessionStats.value = SessionStats(
                peakWatts = snapshot.watts,
                peakCurrentMa = abs(snapshot.currentMa),
                averageWatts = snapshot.watts,
                minTempC = snapshot.temperatureC,
                maxTempC = snapshot.temperatureC,
                startLevel = snapshot.levelPercent,
                connectedAtMs = if (snapshot.isPlugged) snapshot.timestampMs else null,
                sampleCount = 1
            )
            return
        }

        val count = current.sampleCount + 1
        cumulativeWatts += snapshot.watts
        val minT = if (current.sampleCount == 0) snapshot.temperatureC else minOf(current.minTempC, snapshot.temperatureC)
        val maxT = if (current.sampleCount == 0) snapshot.temperatureC else maxOf(current.maxTempC, snapshot.temperatureC)

        _sessionStats.value = current.copy(
            peakWatts = maxOf(current.peakWatts, snapshot.watts),
            peakCurrentMa = maxOf(current.peakCurrentMa, abs(snapshot.currentMa)),
            averageWatts = cumulativeWatts / count.toDouble(),
            minTempC = minT,
            maxTempC = maxT,
            startLevel = current.startLevel ?: snapshot.levelPercent,
            connectedAtMs = current.connectedAtMs ?: if (snapshot.isPlugged) snapshot.timestampMs else null,
            sampleCount = count
        )
    }

    companion object {
        private const val MAX_SAMPLES = 60
    }
}
