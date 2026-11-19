package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ota.OtaNotificationHelper
import com.example.ui.navigation.HardikBudsProRoot
import com.example.viewmodel.BudsViewModel

class MainActivity : ComponentActivity() {
    private val budsViewModel: BudsViewModel by viewModels()
    private var openUpdateCenterOnLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openUpdateCenterOnLaunch = intent?.getBooleanExtra(OtaNotificationHelper.EXTRA_OPEN_UPDATE_CENTER, false) == true
        setContent {
            HardikBudsProRoot(
                viewModel = budsViewModel,
                openUpdateCenterInitially = openUpdateCenterOnLaunch,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OtaNotificationHelper.EXTRA_OPEN_UPDATE_CENTER, false)) {
            openUpdateCenterOnLaunch = true
        }
    }

    override fun onResume() {
        super.onResume()
        budsViewModel.refreshPermissions()
        budsViewModel.refreshInstallPermissionState()
        budsViewModel.bluetoothController.refreshBondedAndConnectedDevices()
        budsViewModel.audioEngine.refreshPlaybackAndVolume()
    }
}
