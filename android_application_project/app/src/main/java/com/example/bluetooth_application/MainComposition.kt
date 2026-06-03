package com.example.bluetooth_application

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainComposition {
    companion object {

        //suppress for scanned device list
        //user must have bluetooth enabled and permissions granted
        @SuppressLint("MissingPermission")
        @Composable
        fun Comp(
            onPermissionClick: () -> Unit,
            modifier: Modifier = Modifier,
            context: Context,
            bluetoothClass: BluetoothClass
        ) {
            Column(modifier = modifier.padding(6.dp).background(color = MaterialTheme.colorScheme.background)) {

                val containerModifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                    .border(width = 4.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
                    .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                    .fillMaxWidth()

                //Row with bluetooth buttons
                Row(modifier = containerModifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    val buttonModifier = Modifier.padding(10.dp).width(140.dp)
                    Button(onClick = if(bluetoothClass.ready.value){
                        {
                            if(bluetoothClass.ready.value){
                                bluetoothClass.startScan()
                            }else{
                                Toast.makeText(context, "Adapter or permissions error!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }else{onPermissionClick},
                        modifier = buttonModifier,
                        enabled = !(bluetoothClass.currentlyScanning.value && bluetoothClass.ready.value),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ){
                        Text(text = "Scan")
                    }
                    Button(onClick = {
                        if(bluetoothClass.ready.value){
                            bluetoothClass.stopScan()
                        }else{
                            Toast.makeText(context, "Adapter or permissions error!", Toast.LENGTH_SHORT).show()
                        }
                    },
                        modifier = buttonModifier,
                        enabled = bluetoothClass.currentlyScanning.value,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ){
                        Text(text = "Stop scanning")
                    }
                }

                //Column with bluetooth devices
                Column(modifier = containerModifier.fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    val contentModifier = Modifier.padding(10.dp)
                        .border(width = 4.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
                        .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                        .height(165.dp)
                        .fillMaxWidth()

                    //printing all devices
                    for(i in 0 until bluetoothClass.deviceList.size) {

                        Row(
                            modifier = contentModifier,
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val buttonModifier = Modifier.padding(5.dp).width(150.dp)
                            val textModifier = Modifier.padding(5.dp)

                            //content
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier.weight(64f).padding(start = 5.dp).fillMaxWidth()
                            ) {
                                Text(text = "Name:   " + bluetoothClass.deviceList[i].device.name, modifier = textModifier, color = MaterialTheme.colorScheme.secondary)
                                Text(text = "MAC:    " + bluetoothClass.deviceList[i].device.address, modifier = textModifier, color = MaterialTheme.colorScheme.secondary)
                                Text(text = "Temperature:   " + bluetoothClass.deviceList[i].temperature.value + " " + Char(176), modifier = textModifier, color = MaterialTheme.colorScheme.secondary)
                                Text(text = "Humidity:          " + bluetoothClass.deviceList[i].humidity.value + " %", modifier = textModifier, color = MaterialTheme.colorScheme.secondary)
                            }

                            //buttons
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier.weight(36f).fillMaxWidth()
                            ) {
                                Text(text = if(bluetoothClass.deviceList[i].isConnected.value){
                                    "Connected"
                                }else{
                                    "Disconnected"
                                }
                                    , modifier = textModifier, color = MaterialTheme.colorScheme.secondary)

                                if(!bluetoothClass.deviceList[i].isConnected.value) {
                                    Button(
                                        onClick = { bluetoothClass.connect(i, context) },
                                        modifier = buttonModifier,
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Text(text = "Connect")
                                    }
                                }else{
                                    Button(
                                        onClick = { bluetoothClass.disconnect(i, context) },
                                        modifier = buttonModifier,
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Text(text = "Disconnect")
                                    }
                                }

                                Button(onClick = {bluetoothClass.refresh(i, context)},
                                    modifier = buttonModifier,
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    enabled = bluetoothClass.deviceList[i].isConnected.value
                                ) {
                                    Text(text = "Refresh")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}