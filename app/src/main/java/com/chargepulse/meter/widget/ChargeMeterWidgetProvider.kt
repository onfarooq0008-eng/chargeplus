package com.chargepulse.meter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.chargepulse.meter.ChargePulseApp
import com.chargepulse.meter.MainActivity
import com.chargepulse.meter.R
import com.chargepulse.meter.data.BatteryMetricsCalculator
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.ChargingSpeedTier

class ChargeMeterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as ChargePulseApp
        val telemetry = app.repository.refreshNow()
        val useFahrenheit = app.settingsStore.current.useFahrenheit
        appWidgetIds.forEach { widgetId ->
            val views = ChargeMeterWidgetHelper.buildRemoteViews(context, telemetry, useFahrenheit)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val app = context.applicationContext as ChargePulseApp
            val telemetry = app.repository.refreshNow()
            ChargeMeterWidgetHelper.updateAllWidgets(
                context = context,
                telemetry = telemetry,
                useFahrenheit = app.settingsStore.current.useFahrenheit
            )
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.chargepulse.meter.ACTION_REFRESH_WIDGET"
    }
}

object ChargeMeterWidgetHelper {

    fun updateAllWidgets(
        context: Context,
        telemetry: BatteryTelemetry,
        useFahrenheit: Boolean
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ChargeMeterWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val views = buildRemoteViews(context, telemetry, useFahrenheit)
        manager.updateAppWidget(widgetIds, views)
    }

    fun hasPinnedWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, ChargeMeterWidgetProvider::class.java)
        return manager.getAppWidgetIds(component).isNotEmpty()
    }

    fun requestPinWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, ChargeMeterWidgetProvider::class.java)
        return manager.requestPinAppWidget(provider, null, null)
    }

    fun buildRemoteViews(
        context: Context,
        telemetry: BatteryTelemetry,
        useFahrenheit: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_charge_meter)

        val bgRes = when {
            telemetry.isFastCharging -> R.drawable.widget_bg_fast_charging
            telemetry.isPlugged -> R.drawable.widget_bg_charging
            else -> R.drawable.widget_bg_standby
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)

        val badgeColor = when (telemetry.speedTier) {
            ChargingSpeedTier.SUPER_FAST -> Color.parseColor("#00D2FF")
            ChargingSpeedTier.FAST -> Color.parseColor("#00E676")
            ChargingSpeedTier.STANDARD, ChargingSpeedTier.SLOW -> Color.parseColor("#FFB300")
            ChargingSpeedTier.FULL -> Color.parseColor("#00E676")
            else -> Color.parseColor("#9FB0C7")
        }

        val badgeText = if (telemetry.isPlugged) {
            "⚡ ${telemetry.speedTier.shortBadge}"
        } else {
            telemetry.speedTier.shortBadge
        }

        views.setTextViewText(R.id.widget_badge_text, badgeText)
        views.setTextColor(R.id.widget_badge_text, badgeColor)
        views.setTextViewText(R.id.widget_source_text, telemetry.chargingSource.displayLabel)

        views.setTextViewText(
            R.id.widget_watts_value,
            BatteryMetricsCalculator.formatWatts(telemetry.watts)
        )
        views.setTextColor(R.id.widget_watts_value, badgeColor)

        views.setTextViewText(
            R.id.widget_amps_value,
            BatteryMetricsCalculator.formatMilliAmpsCompact(telemetry.currentMa)
        )
        views.setTextViewText(
            R.id.widget_voltage_value,
            BatteryMetricsCalculator.formatVoltage(telemetry.voltageV)
        )

        views.setTextViewText(R.id.widget_battery_percent, "${telemetry.levelPercent}%")
        views.setProgressBar(R.id.widget_battery_progress, 100, telemetry.levelPercent, false)

        val etaText = when {
            telemetry.speedTier == ChargingSpeedTier.FULL -> "Charged 100%"
            telemetry.isCharging -> BatteryMetricsCalculator.formatDurationEta(telemetry.estimatedTimeToFullMs)
            telemetry.isPlugged -> "Connected (Idle)"
            else -> "Unplugged"
        }
        views.setTextViewText(R.id.widget_eta_text, etaText)

        val effectiveHealthPct = telemetry.hardwareStateOfHealthPercent ?: telemetry.estimatedHealthPercent
        val healthText = BatteryMetricsCalculator.healthLabelSummary(
            health = telemetry.healthStatus,
            tempC = telemetry.temperatureC,
            useFahrenheit = useFahrenheit,
            healthPercent = effectiveHealthPct
        )
        views.setTextViewText(R.id.widget_health_text, healthText)

        // Tap anywhere on widget to open ChargePulse dashboard
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPending)

        // Tap refresh icon on widget for instant telemetry update
        val refreshIntent = Intent(context, ChargeMeterWidgetProvider::class.java).apply {
            action = ChargeMeterWidgetProvider.ACTION_REFRESH_WIDGET
        }
        val refreshPending = PendingIntent.getBroadcast(
            context,
            101,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

        return views
    }
}
