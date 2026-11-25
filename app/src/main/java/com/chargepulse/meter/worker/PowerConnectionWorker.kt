package com.chargepulse.meter.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.chargepulse.meter.ChargePulseApp
import com.chargepulse.meter.widget.ChargeMeterWidgetHelper
import java.util.concurrent.TimeUnit

/**
 * Android 14+ compliant WorkManager task with charging constraint so the home screen widget
 * automatically refreshes when power connects even if the persistent foreground monitor is off.
 */
class PowerConnectionWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val app = applicationContext as ChargePulseApp
        val telemetry = app.repository.refreshNow()
        ChargeMeterWidgetHelper.updateAllWidgets(
            context = applicationContext,
            telemetry = telemetry,
            useFahrenheit = app.settingsStore.current.useFahrenheit
        )
        if (!telemetry.isPlugged) {
            enqueueChargingTrigger(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_ONETIME_WORK = "chargepulse_power_connect_trigger"
        private const val UNIQUE_PERIODIC_WORK = "chargepulse_periodic_charging_sync"

        fun enqueueChargingTrigger(context: Context) {
            val chargingConstraints = Constraints.Builder()
                .setRequiresCharging(true)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<PowerConnectionWorker>()
                .setConstraints(chargingConstraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONETIME_WORK,
                ExistingWorkPolicy.KEEP,
                oneTimeRequest
            )

            val periodicRequest = PeriodicWorkRequestBuilder<PowerConnectionWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(chargingConstraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
