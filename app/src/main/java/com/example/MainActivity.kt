package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.HardikBudsProRoot
import com.example.viewmodel.BudsViewModel

class MainActivity : ComponentActivity() {
    private val budsViewModel: BudsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HardikBudsProRoot(viewModel = budsViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        budsViewModel.refreshPermissions()
        budsViewModel.bluetoothController.refreshBondedAndConnectedDevices()
        budsViewModel.audioEngine.refreshPlaybackAndVolume()
    }
}
