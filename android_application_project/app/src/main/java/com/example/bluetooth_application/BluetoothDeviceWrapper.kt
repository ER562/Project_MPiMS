package com.example.bluetooth_application

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf

class BluetoothDeviceWrapper(var device: BluetoothDevice) {
    var isConnected: MutableState<Boolean> = mutableStateOf(false)
    var bluetoothGatt: BluetoothGatt? = null
    var temperature: MutableState<Double> = mutableDoubleStateOf(0.0)
    var humidity: MutableState<Double> = mutableDoubleStateOf(0.0)
}