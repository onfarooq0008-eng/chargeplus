package com.chargepulse.meter.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chargepulse.meter.ChargePulseApp
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.MonitorSettings
import com.chargepulse.meter.model.SamplePoint
import com.chargepulse.meter.model.SessionStats
import com.chargepulse.meter.service.ChargingMonitorService
import com.chargepulse.meter.widget.ChargeMeterWidgetHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeIntegrationState(
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val hasPinnedWidget: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ChargePulseApp
    private val repository = app.repository
    private val settingsStore = app.settingsStore

    val telemetry: StateFlow<BatteryTelemetry> = repository.telemetry
    val samples: StateFlow<List<SamplePoint>> = repository.samples
    val sessionStats: StateFlow<SessionStats> = repository.sessionStats
    val settings: StateFlow<MonitorSettings> = settingsStore.settings

    private val _homeState = MutableStateFlow(HomeIntegrationState())
    val homeState: StateFlow<HomeIntegrationState> = _homeState.asStateFlow()

    init {
        startForegroundPollingLoop()
    }

    private fun startForegroundPollingLoop() {
        viewModelScope.launch {
            while (isActive) {
                val intervalMs = settingsStore.current.samplingIntervalMs.coerceAtLeast(500L)
                val snapshot = repository.refreshNow()
                ChargeMeterWidgetHelper.updateAllWidgets(
                    context = app,
                    telemetry = snapshot,
                    useFahrenheit = settingsStore.current.useFahrenheit
                )
                delay(intervalMs)
            }
        }
    }

    fun refreshNow() {
        val snapshot = repository.refreshNow()
        ChargeMeterWidgetHelper.updateAllWidgets(
            context = app,
            telemetry = snapshot,
            useFahrenheit = settingsStore.current.useFahrenheit
        )
    }

    fun refreshPermissionAndWidgetStatus(hasNotifications: Boolean) {
        _homeState.value = HomeIntegrationState(
            hasOverlayPermission = Settings.canDrawOverlays(app),
            hasNotificationPermission = hasNotifications,
            hasPinnedWidget = ChargeMeterWidgetHelper.hasPinnedWidgets(app)
        )
    }

    fun ensureMonitorServiceMatchesPreference() {
        if (settingsStore.current.backgroundMonitorEnabled) {
            ChargingMonitorService.startSafely(app)
        }
    }

    fun setBackgroundMonitorEnabled(enabled: Boolean) {
        settingsStore.update { it.copy(backgroundMonitorEnabled = enabled) }
        if (enabled) {
            ChargingMonitorService.startSafely(app)
        } else {
            ChargingMonitorService.stop(app)
        }
    }

    fun setAutoShowOverlay(enabled: Boolean) {
        settingsStore.update { it.copy(autoShowOverlayOnConnect = enabled) }
    }

    fun setOverlayAutoDismissSeconds(seconds: Int) {
        settingsStore.update { it.copy(overlayAutoDismissSeconds = seconds) }
    }

    fun setInvertCurrentPolarity(invert: Boolean) {
        settingsStore.update { it.copy(invertCurrentPolarity = invert) }
        refreshNow()
    }

    fun setUseFahrenheit(useFahrenheit: Boolean) {
        settingsStore.update { it.copy(useFahrenheit = useFahrenheit) }
        refreshNow()
    }

    fun setDesignCapacityMah(capacityMah: Int) {
        settingsStore.update { it.copy(designCapacityMah = capacityMah.coerceIn(1500, 15000)) }
        refreshNow()
    }

    fun setFastChargeThresholds(fastW: Float, superFastW: Float) {
        val safeFast = fastW.coerceIn(6f, 40f)
        val safeSuper = superFastW.coerceAtLeast(safeFast + 3f).coerceAtMost(120f)
        settingsStore.update {
            it.copy(
                fastChargeThresholdWatts = safeFast,
                superFastChargeThresholdWatts = safeSuper
            )
        }
        refreshNow()
    }

    fun previewHomeOverlay(): Boolean {
        if (!Settings.canDrawOverlays(app)) return false
        return ChargingMonitorService.startSafely(
            app,
            ChargingMonitorService.ACTION_SHOW_OVERLAY_PREVIEW
        )
    }

    fun pinHomeScreenWidget(): Boolean {
        val requested = ChargeMeterWidgetHelper.requestPinWidget(app)
        refreshNow()
        return requested
    }
}
