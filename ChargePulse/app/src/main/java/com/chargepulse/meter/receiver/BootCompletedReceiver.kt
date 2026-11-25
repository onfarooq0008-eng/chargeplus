package com.chargepulse.meter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chargepulse.meter.ChargePulseApp
import com.chargepulse.meter.service.ChargingMonitorService
import com.chargepulse.meter.widget.ChargeMeterWidgetHelper
import com.chargepulse.meter.worker.PowerConnectionWorker

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            PowerConnectionWorker.enqueueChargingTrigger(context)

            val app = context.applicationContext as ChargePulseApp
            val snapshot = app.repository.refreshNow()
            ChargeMeterWidgetHelper.updateAllWidgets(
                context = context,
                telemetry = snapshot,
                useFahrenheit = app.settingsStore.current.useFahrenheit
            )

            if (app.settingsStore.current.backgroundMonitorEnabled) {
                // Attempt starting foreground monitor safely; Android 14/15 restrictions are handled gracefully.
                ChargingMonitorService.startSafely(context)
            }
        }
    }
}
