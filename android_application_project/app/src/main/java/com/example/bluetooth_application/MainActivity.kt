package com.example.bluetooth_application

import android.bluetooth.BluetoothAdapter
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.bluetooth_application.ui.theme.Bluetooth_applicationTheme

class MainActivity : ComponentActivity() {

    val bluetooth: BluetoothClass by viewModels()

    //launcher for asking permissions
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied!", Toast.LENGTH_SHORT).show()
        }
    }

    //launcher for enabling adapter
    private val requestAdapterTurnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Adapter turned on!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Adapter is turned off!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(bluetooth.ready(this)){
            bluetooth.ready.value = true
        }
        enableEdgeToEdge()
        setContent {
            Bluetooth_applicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainComposition.Comp(
                        onPermissionClick = {
                            bluetooth.turnOn(requestAdapterTurnLauncher, mainContext = this)
                            bluetooth.askForPermissions(requestPermissionLauncher, mainContext = this)
                        },
                        modifier = Modifier.padding(innerPadding),
                        context = this,
                        bluetoothClass = bluetooth
                    )
                }
            }
        }
    }

    override fun onStart(){
        super.onStart()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetooth.stateReceiver, filter)
    }

    override fun onStop(){
        super.onStop()
        unregisterReceiver(bluetooth.stateReceiver)
    }
}