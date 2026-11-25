package com.chargepulse.meter.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.chargepulse.meter.ChargePulseApp
import com.chargepulse.meter.MainActivity
import com.chargepulse.meter.R
import com.chargepulse.meter.data.BatteryMetricsCalculator
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.widget.ChargeMeterWidgetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground monitoring service that receives instantaneous ACTION_POWER_CONNECTED /
 * ACTION_POWER_DISCONNECTED broadcasts, ticks live Watts & Amperes while charging,
 * updates the Android Home Screen Widget, and shows the optional Home Page HUD overlay.
 */
class ChargingMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private lateinit var overlayManager: ChargingOverlayManager

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val app = applicationContext as ChargePulseApp
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    overlayManager.resetSessionDismissal()
                    val snapshot = app.repository.refreshNow()
                    handleTelemetryTick(snapshot, powerJustConnected = true)
                    startLiveTicker()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    overlayManager.dismiss(markSessionDismissed = false)
                    val snapshot = app.repository.refreshNow()
                    handleTelemetryTick(snapshot, powerJustConnected = false)
                    stopLiveTicker()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val snapshot = app.repository.refreshNow(intent)
                    handleTelemetryTick(snapshot, powerJustConnected = false)
                    if (snapshot.isPlugged) {
                        ensureTickerRunning()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    val snapshot = app.repository.refreshNow()
                    handleTelemetryTick(snapshot, powerJustConnected = false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlayManager = ChargingOverlayManager(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            powerReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_MONITOR) {
            stopLiveTicker()
            overlayManager.dismiss()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val app = applicationContext as ChargePulseApp
        val initialSnapshot = app.repository.refreshNow()
        val notification = buildNotification(initialSnapshot, app.settingsStore.current.useFahrenheit)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (action == ACTION_SHOW_OVERLAY_PREVIEW) {
            overlayManager.resetSessionDismissal()
            overlayManager.showOrUpdate(
                telemetry = initialSnapshot,
                useFahrenheit = app.settingsStore.current.useFahrenheit,
                autoDismissSeconds = app.settingsStore.current.overlayAutoDismissSeconds,
                forceShow = true
            )
        }

        handleTelemetryTick(initialSnapshot, powerJustConnected = false)
        if (initialSnapshot.isPlugged) {
            startLiveTicker()
        }

        return START_NOT_STICKY
    }

    private fun ensureTickerRunning() {
        if (tickerJob?.isActive != true) {
            startLiveTicker()
        }
    }

    private fun startLiveTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val app = applicationContext as ChargePulseApp
            while (isActive) {
                val intervalMs = app.settingsStore.current.samplingIntervalMs.coerceAtLeast(500L)
                delay(intervalMs)
                val snapshot = app.repository.refreshNow()
                handleTelemetryTick(snapshot, powerJustConnected = false)
                if (!snapshot.isPlugged) {
                    break
                }
            }
        }
    }

    private fun stopLiveTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun handleTelemetryTick(
        snapshot: BatteryTelemetry,
        powerJustConnected: Boolean
    ) {
        val app = applicationContext as ChargePulseApp
        val settings = app.settingsStore.current

        ChargeMeterWidgetHelper.updateAllWidgets(
            context = this,
            telemetry = snapshot,
            useFahrenheit = settings.useFahrenheit
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(snapshot, settings.useFahrenheit)
        )

        if (powerJustConnected && settings.autoShowOverlayOnConnect) {
            overlayManager.showOrUpdate(
                telemetry = snapshot,
                useFahrenheit = settings.useFahrenheit,
                autoDismissSeconds = settings.overlayAutoDismissSeconds,
                forceShow = false
            )
        } else if (overlayManager.isShowing) {
            overlayManager.updateIfVisible(snapshot, settings.useFahrenheit)
        }
    }

    private fun buildNotification(
        telemetry: BatteryTelemetry,
        useFahrenheit: Boolean
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            200,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wattsStr = BatteryMetricsCalculator.formatWatts(telemetry.watts)
        val ampsStr = BatteryMetricsCalculator.formatAmpsAndMilliAmps(telemetry.currentMa)
        val tempStr = BatteryMetricsCalculator.formatTemperature(telemetry.temperatureC, useFahrenheit)

        val title = if (telemetry.isPlugged) {
            "⚡ ${telemetry.speedTier.label} • $wattsStr ($ampsStr)"
        } else {
            "ChargePulse Ready • ${telemetry.levelPercent}% (${telemetry.healthStatus.label})"
        }

        val body = if (telemetry.isPlugged) {
            "${telemetry.levelPercent}% • ${telemetry.chargingSource.displayLabel} • Health: ${telemetry.healthStatus.label} ($tempStr)"
        } else {
            "Waiting for charger • Draw: $wattsStr ($ampsStr) • $tempStr"
        }

        return NotificationCompat.Builder(this, ChargePulseApp.CHANNEL_CHARGING_MONITOR)
            .setSmallIcon(R.drawable.ic_bolt)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingOpen)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopLiveTicker()
        overlayManager.dismiss()
        try {
            unregisterReceiver(powerReceiver)
        } catch (_: Exception) {
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4041
        const val ACTION_START_MONITOR = "com.chargepulse.meter.ACTION_START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.chargepulse.meter.ACTION_STOP_MONITOR"
        const val ACTION_SHOW_OVERLAY_PREVIEW = "com.chargepulse.meter.ACTION_SHOW_OVERLAY_PREVIEW"

        fun startSafely(context: Context, action: String = ACTION_START_MONITOR): Boolean {
            val intent = Intent(context, ChargingMonitorService::class.java).apply {
                this.action = action
            }
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChargingMonitorService::class.java).apply {
                action = ACTION_STOP_MONITOR
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
