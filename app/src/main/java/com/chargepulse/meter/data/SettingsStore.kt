package com.chargepulse.meter.data

import android.content.Context
import android.content.SharedPreferences
import com.chargepulse.meter.model.MonitorSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<MonitorSettings> = _settings.asStateFlow()

    val current: MonitorSettings
        get() = _settings.value

    private fun readSettings(): MonitorSettings {
        val detectedDefaultCap = prefs.getInt(KEY_DESIGN_CAPACITY_MAH, 5000)
        return MonitorSettings(
            autoShowOverlayOnConnect = prefs.getBoolean(KEY_AUTO_OVERLAY, true),
            overlayAutoDismissSeconds = prefs.getInt(KEY_OVERLAY_DISMISS_SEC, 15),
            backgroundMonitorEnabled = prefs.getBoolean(KEY_BG_MONITOR, true),
            invertCurrentPolarity = prefs.getBoolean(KEY_INVERT_POLARITY, false),
            useFahrenheit = prefs.getBoolean(KEY_USE_FAHRENHEIT, false),
            designCapacityMah = detectedDefaultCap,
            fastChargeThresholdWatts = prefs.getFloat(KEY_FAST_THRESHOLD_W, 12f),
            superFastChargeThresholdWatts = prefs.getFloat(KEY_SUPER_FAST_THRESHOLD_W, 25f),
            samplingIntervalMs = prefs.getLong(KEY_SAMPLING_INTERVAL_MS, 1000L)
        )
    }

    fun setInitialDetectedCapacityIfUnset(detectedMah: Int) {
        if (!prefs.contains(KEY_DESIGN_CAPACITY_MAH) && detectedMah in 1500..15000) {
            update { it.copy(designCapacityMah = detectedMah) }
        }
    }

    fun update(transform: (MonitorSettings) -> MonitorSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putBoolean(KEY_AUTO_OVERLAY, next.autoShowOverlayOnConnect)
            .putInt(KEY_OVERLAY_DISMISS_SEC, next.overlayAutoDismissSeconds)
            .putBoolean(KEY_BG_MONITOR, next.backgroundMonitorEnabled)
            .putBoolean(KEY_INVERT_POLARITY, next.invertCurrentPolarity)
            .putBoolean(KEY_USE_FAHRENHEIT, next.useFahrenheit)
            .putInt(KEY_DESIGN_CAPACITY_MAH, next.designCapacityMah)
            .putFloat(KEY_FAST_THRESHOLD_W, next.fastChargeThresholdWatts)
            .putFloat(KEY_SUPER_FAST_THRESHOLD_W, next.superFastChargeThresholdWatts)
            .putLong(KEY_SAMPLING_INTERVAL_MS, next.samplingIntervalMs)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "chargepulse_settings"
        private const val KEY_AUTO_OVERLAY = "auto_show_overlay"
        private const val KEY_OVERLAY_DISMISS_SEC = "overlay_dismiss_sec"
        private const val KEY_BG_MONITOR = "bg_monitor_enabled"
        private const val KEY_INVERT_POLARITY = "invert_polarity"
        private const val KEY_USE_FAHRENHEIT = "use_fahrenheit"
        private const val KEY_DESIGN_CAPACITY_MAH = "design_capacity_mah"
        private const val KEY_FAST_THRESHOLD_W = "fast_threshold_w"
        private const val KEY_SUPER_FAST_THRESHOLD_W = "super_fast_threshold_w"
        private const val KEY_SAMPLING_INTERVAL_MS = "sampling_interval_ms"
    }
}
