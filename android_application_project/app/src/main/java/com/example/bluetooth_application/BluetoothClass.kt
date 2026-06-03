package com.example.bluetooth_application

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import java.util.UUID

class BluetoothClass : ViewModel() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    val deviceList = mutableStateListOf<BluetoothDeviceWrapper>()
    var ready = mutableStateOf(false)
    var currentlyScanning = mutableStateOf(false)

    //filters and settings for scanning
    val filter: ScanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000181a-0000-1000-8000-00805f9b34fb")).build()
    val settings: ScanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    val filterArray: ArrayList<ScanFilter> = arrayListOf(filter)

    //scanner
    val callback = MyScanCallback()
    val leScanner get() = bluetoothAdapter?.bluetoothLeScanner

    //constant
    val alertUuid: UUID = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb")
    val humidityUuid: UUID = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")
    val temperatureUuid: UUID = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")
    val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")



    //required permissions depending on SDK version
    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    //registering for bluetooth updates
    val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?){
            if(intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                if(context != null) {
                    ready.value = ready(context)
                    if(!ready.value){
                        currentlyScanning.value = false
                    }
                }else{
                    ready.value = false
                    currentlyScanning.value = false
                }
            }
        }
    }

    //callback for scanning
    inner class MyScanCallback : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult){
            var duplicate = false
            for(i in 0 until deviceList.size){
                if(deviceList[i].device == result.device){
                    duplicate = true
                    break
                }
            }
            if(!duplicate){
                deviceList.add(BluetoothDeviceWrapper(result.device))
            }
        }
    }

    //proper bluetooth scanning
    //before calling this function you must check for permissions
    @SuppressLint("MissingPermission")
    fun startScan() {
        leScanner?.startScan(filterArray, settings, callback)
        currentlyScanning.value = true
    }

    //before calling this function you must check for permissions
    @SuppressLint("MissingPermission")
    fun stopScan() {
        leScanner?.stopScan(callback)
        currentlyScanning.value = false
    }

    fun turnOn(launcher: ActivityResultLauncher<Intent>, mainContext: Context):Boolean{
        val bluetoothManager: BluetoothManager = mainContext.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if(bluetoothAdapter?.isEnabled == false){
            launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        ready.value = ready(mainContext)

        return bluetoothAdapter?.isEnabled == true
    }

    fun askForPermissions(launcher: ActivityResultLauncher<Array<String>>, mainContext: Context):Boolean{
        launcher.launch(requiredPermissions)

        ready.value = ready(mainContext)

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(mainContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    //bluetooth adapter must be enabled and all permissions must have been granted
    fun ready(mainContext: Context):Boolean{
        return bluetoothAdapter?.isEnabled == true && requiredPermissions.all {
            ContextCompat.checkSelfPermission(mainContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    //connection
    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                var deviceWrapper = deviceList.find { it.device == gatt.device }
                if (bluetoothAdapter?.isEnabled == true) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        deviceWrapper?.isConnected?.value = true
                        gatt.discoverServices()
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        deviceWrapper?.isConnected?.value = false
                        gatt.close()
                    }
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                var service = gatt?.getService(UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb"))
                var characteristicList = service?.characteristics
                if(characteristicList != null){
                    if(characteristicList.size == 3) {
                        for (i in 0 until characteristicList.size) {
                            if (characteristicList[i].uuid != temperatureUuid &&
                                characteristicList[i].uuid != humidityUuid &&
                                characteristicList[i].uuid != alertUuid
                            ) {
                                gatt?.disconnect()
                                return
                            }
                        }
                        for (i in 0 until characteristicList.size) {
                            if (characteristicList[i].uuid == temperatureUuid) {
                                subscribeToCharacteristic(gatt, characteristicList[i])
                            }
                        }
                    }
                }
            }
        }


        @Deprecated("Needed for older versions of android")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if(characteristic?.uuid == temperatureUuid){
                var deviceWrapper = deviceList.find { it.device == gatt?.device }
                @Suppress("DEPRECATION")
                var value = characteristic.value
                deviceWrapper?.temperature?.value = ((value[0].toInt() and 0xff) + (value[1].toInt() shl 8)).toDouble()
            }else if(characteristic?.uuid == humidityUuid){
                var deviceWrapper = deviceList.find { it.device == gatt?.device }
                @Suppress("DEPRECATION")
                var value = characteristic.value
                deviceWrapper?.humidity?.value = ((value[0].toInt() and 0xff) + (value[1].toInt() shl 8)).toDouble()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if(characteristic.uuid == temperatureUuid){
                var deviceWrapper = deviceList.find { it.device == gatt.device }
                deviceWrapper?.temperature?.value = ((value[0].toInt() and 0xff) + (value[1].toInt() shl 8)).toDouble()
            }else if(characteristic.uuid == humidityUuid){
                var deviceWrapper = deviceList.find { it.device == gatt.device }
                deviceWrapper?.humidity?.value = ((value[0].toInt() and 0xff) + (value[1].toInt() shl 8)).toDouble()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(descriptor?.characteristic?.uuid == temperatureUuid){
                    var service = gatt?.getService(UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb"))
                    var characteristic = service?.characteristics?.find{it.uuid == humidityUuid}
                    if(characteristic != null) {
                        subscribeToCharacteristic(gatt, characteristic)
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(deviceListNumber: Int, mainContext: Context){
        if(ready(mainContext)) {
            deviceList[deviceListNumber].bluetoothGatt = deviceList[deviceListNumber].device.connectGatt(mainContext, false, gattCallback)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    //implement
    fun disconnect(deviceListNumber: Int, mainContext: Context){
        if(ready(mainContext)) {
            deviceList[deviceListNumber].bluetoothGatt?.disconnect()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun refresh(deviceListNumber: Int, mainContext: Context){
        if(ready(mainContext)) {
            val service = deviceList[deviceListNumber].bluetoothGatt?.getService(UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb"))
            val characteristic = service?.characteristics?.find{it.uuid == alertUuid}
            if(characteristic != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    deviceList[deviceListNumber].bluetoothGatt?.writeCharacteristic(
                        characteristic,
                        byteArrayOf(1),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = byteArrayOf(1)
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    deviceList[deviceListNumber].bluetoothGatt?.writeCharacteristic(characteristic)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun subscribeToCharacteristic(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic){
        if(gatt == null){
            return
        }
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor != null) {
            val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }
}