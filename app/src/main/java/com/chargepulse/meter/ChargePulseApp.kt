package com.chargepulse.meter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.chargepulse.meter.data.BatteryMonitorRepository
import com.chargepulse.meter.data.SettingsStore
import com.chargepulse.meter.worker.PowerConnectionWorker

class ChargePulseApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var repository: BatteryMonitorRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        repository = BatteryMonitorRepository(this, settingsStore)
        createNotificationChannel()
        PowerConnectionWorker.enqueueChargingTrigger(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_CHARGING_MONITOR,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_CHARGING_MONITOR = "chargepulse_live_monitor"
    }
}
