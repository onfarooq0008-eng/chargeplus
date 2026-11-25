package com.chargepulse.meter.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.chargepulse.meter.MainActivity
import com.chargepulse.meter.R
import com.chargepulse.meter.data.BatteryMetricsCalculator
import com.chargepulse.meter.model.BatteryTelemetry
import com.chargepulse.meter.model.ChargingSpeedTier
import kotlin.math.abs

/**
 * Displays a draggable floating HUD card directly on the Android Home Page (and over apps)
 * when a charger is connected, provided SYSTEM_ALERT_WINDOW permission has been granted.
 */
class ChargingOverlayManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var dismissedForCurrentSession = false

    private val autoDismissRunnable = Runnable {
        dismissInternal(markSessionDismissed = true)
    }

    val isShowing: Boolean
        get() = overlayView != null

    fun resetSessionDismissal() {
        dismissedForCurrentSession = false
    }

    fun showOrUpdate(
        telemetry: BatteryTelemetry,
        useFahrenheit: Boolean,
        autoDismissSeconds: Int,
        forceShow: Boolean = false
    ) {
        if (!Settings.canDrawOverlays(appContext)) return
        if (!forceShow && dismissedForCurrentSession) return

        mainHandler.post {
            if (overlayView == null) {
                createAndAttachOverlay()
            }
            bindTelemetry(telemetry, useFahrenheit)
            scheduleAutoDismissIfNeeded(autoDismissSeconds)
        }
    }

    fun updateIfVisible(telemetry: BatteryTelemetry, useFahrenheit: Boolean) {
        if (overlayView == null) return
        mainHandler.post {
            if (overlayView != null) {
                bindTelemetry(telemetry, useFahrenheit)
            }
        }
    }

    fun dismiss(markSessionDismissed: Boolean = false) {
        mainHandler.post {
            dismissInternal(markSessionDismissed)
        }
    }

    private fun dismissInternal(markSessionDismissed: Boolean) {
        mainHandler.removeCallbacks(autoDismissRunnable)
        if (markSessionDismissed) {
            dismissedForCurrentSession = true
        }
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // View was already detached
            }
        }
        overlayView = null
        layoutParams = null
    }

    private fun scheduleAutoDismissIfNeeded(autoDismissSeconds: Int) {
        mainHandler.removeCallbacks(autoDismissRunnable)
        if (autoDismissSeconds > 0) {
            mainHandler.postDelayed(autoDismissRunnable, autoDismissSeconds * 1000L)
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun createAndAttachOverlay() {
        val inflater = LayoutInflater.from(appContext)
        val view = inflater.inflate(R.layout.overlay_charging_hud, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 140
        }

        val closeBtn = view.findViewById<ImageButton>(R.id.overlay_btn_close)
        closeBtn.setOnClickListener {
            dismissInternal(markSessionDismissed = true)
        }

        // Support dragging card across the screen and tapping to open ChargePulse app
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        view.setOnTouchListener { _, event ->
            val currentParams = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(view, currentParams)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        val openIntent = Intent(appContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        appContext.startActivity(openIntent)
                        dismissInternal(markSessionDismissed = true)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
            layoutParams = params
        } catch (_: Exception) {
            overlayView = null
            layoutParams = null
        }
    }

    private fun bindTelemetry(telemetry: BatteryTelemetry, useFahrenheit: Boolean) {
        val view = overlayView ?: return

        val badgeColor = when (telemetry.speedTier) {
            ChargingSpeedTier.SUPER_FAST -> Color.parseColor("#00D2FF")
            ChargingSpeedTier.FAST -> Color.parseColor("#00E676")
            ChargingSpeedTier.STANDARD, ChargingSpeedTier.SLOW -> Color.parseColor("#FFB300")
            ChargingSpeedTier.FULL -> Color.parseColor("#00E676")
            else -> Color.parseColor("#9FB0C7")
        }

        val badgeText = if (telemetry.isFastCharging) {
            "⚡ ${telemetry.speedTier.shortBadge}"
        } else if (telemetry.isPlugged) {
            "CHARGER CONNECTED • ${telemetry.speedTier.shortBadge}"
        } else {
            "BATTERY MONITOR"
        }

        view.findViewById<TextView>(R.id.overlay_badge_text).apply {
            text = badgeText
            setTextColor(badgeColor)
        }
        view.findViewById<TextView>(R.id.overlay_source_text).text =
            telemetry.chargingSource.displayLabel

        view.findViewById<TextView>(R.id.overlay_watts_value).apply {
            text = BatteryMetricsCalculator.formatWatts(telemetry.watts)
            setTextColor(badgeColor)
        }
        view.findViewById<TextView>(R.id.overlay_amps_value).text =
            BatteryMetricsCalculator.formatMilliAmpsCompact(telemetry.currentMa)
        view.findViewById<TextView>(R.id.overlay_voltage_value).text =
            BatteryMetricsCalculator.formatVoltage(telemetry.voltageV)

        view.findViewById<TextView>(R.id.overlay_battery_percent).text = "${telemetry.levelPercent}%"
        view.findViewById<ProgressBar>(R.id.overlay_battery_progress).progress = telemetry.levelPercent

        val etaText = when {
            telemetry.speedTier == ChargingSpeedTier.FULL -> "Charged"
            telemetry.isCharging -> BatteryMetricsCalculator.formatDurationEta(telemetry.estimatedTimeToFullMs)
            else -> "Standby"
        }
        view.findViewById<TextView>(R.id.overlay_eta_text).text = etaText

        val healthPct = telemetry.hardwareStateOfHealthPercent ?: telemetry.estimatedHealthPercent
        val healthSummary = BatteryMetricsCalculator.healthLabelSummary(
            health = telemetry.healthStatus,
            tempC = telemetry.temperatureC,
            useFahrenheit = useFahrenheit,
            healthPercent = healthPct
        )
        view.findViewById<TextView>(R.id.overlay_health_text).text = "$healthSummary • Tap to open"
    }
}
