package com.chargepulse.meter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chargepulse.meter.ui.MainViewModel
import com.chargepulse.meter.ui.screens.DashboardScreen
import com.chargepulse.meter.ui.screens.HealthScreen
import com.chargepulse.meter.ui.screens.SettingsScreen
import com.chargepulse.meter.ui.theme.ChargePulseColors
import com.chargepulse.meter.ui.theme.ChargePulseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updatePermissionsState()
        viewModel.ensureMonitorServiceMatchesPreference()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionsState()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updatePermissionsState()
        viewModel.ensureMonitorServiceMatchesPreference()

        setContent {
            ChargePulseTheme {
                val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
                val samples by viewModel.samples.collectAsStateWithLifecycle()
                val sessionStats by viewModel.sessionStats.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val homeState by viewModel.homeState.collectAsStateWithLifecycle()

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    containerColor = ChargePulseColors.BgDark,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "ChargePulse",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ChargePulseColors.TextPrimary
                                )
                            },
                            actions = {
                                IconButton(onClick = { viewModel.refreshNow() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh",
                                        tint = ChargePulseColors.CyanSuperFast
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ChargePulseColors.BgDark
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = ChargePulseColors.SurfaceDark) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = "Live Meter"
                                    )
                                },
                                label = { Text("Live Meter") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = "Battery Health"
                                    )
                                },
                                label = { Text("Health") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)

                    when (selectedTab) {
                        0 -> DashboardScreen(
                            telemetry = telemetry,
                            samples = samples,
                            sessionStats = sessionStats,
                            settings = settings,
                            homeState = homeState,
                            onPinWidgetClick = {
                                val ok = viewModel.pinHomeScreenWidget()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) {
                                            "Widget pin dialog opened. Confirm to add ChargePulse to your Home Screen."
                                        } else {
                                            "Long-press your Home Screen -> Widgets -> ChargePulse to add."
                                        }
                                    )
                                }
                            },
                            onRequestOverlayPermission = { openOverlaySettings() },
                            onPreviewOverlayClick = {
                                val launched = viewModel.previewHomeOverlay()
                                if (!launched) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Grant Display Over Other Apps permission first.")
                                    }
                                }
                            },
                            onRequestNotificationPermission = { requestNotificationsIfNeeded() },
                            modifier = contentModifier
                        )
                        1 -> HealthScreen(
                            telemetry = telemetry,
                            sessionStats = sessionStats,
                            settings = settings,
                            modifier = contentModifier
                        )
                        else -> SettingsScreen(
                            settings = settings,
                            homeState = homeState,
                            onToggleBackgroundMonitor = { viewModel.setBackgroundMonitorEnabled(it) },
                            onToggleAutoOverlay = { viewModel.setAutoShowOverlay(it) },
                            onSetOverlayDismissSeconds = { viewModel.setOverlayAutoDismissSeconds(it) },
                            onToggleInvertPolarity = { viewModel.setInvertCurrentPolarity(it) },
                            onToggleFahrenheit = { viewModel.setUseFahrenheit(it) },
                            onSetDesignCapacityMah = { viewModel.setDesignCapacityMah(it) },
                            onSetThresholds = { f, s -> viewModel.setFastChargeThresholds(f, s) },
                            onRequestOverlayPermission = { openOverlaySettings() },
                            onPinWidgetClick = {
                                val ok = viewModel.pinHomeScreenWidget()
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) {
                                            "Widget pin dialog requested!"
                                        } else {
                                            "Long-press your Home Screen -> Widgets -> ChargePulse to add."
                                        }
                                    )
                                }
                            },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionsState()
        viewModel.refreshNow()
    }

    private fun updatePermissionsState() {
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        viewModel.refreshPermissionAndWidgetStatus(hasNotifications = hasNotif)
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
}
