package com.example.share_project.View

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.share_project.DeviceConnectedModel
import com.example.share_project.DeviceRequestModel
import com.example.share_project.DevicesModel
import com.example.share_project.nearbyconnection.NearbyServiceManager

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomePage(
    serviceManager: NearbyServiceManager,
    discoverDevice: List<DevicesModel> = listOf(),
    reqConnection: List<DeviceRequestModel> = listOf(),
    connectedDevices: List<DeviceConnectedModel> = listOf(),
    connectedTap: (DeviceConnectedModel) -> Unit
) {
    var isDiscover by remember { mutableStateOf(false) }
    var isAdvertising by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Nearby Connect") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            Arrangement.Top, Alignment.Start,
        ) {
//                Spacer(modifier = Modifier.padding(top = 16.dp))
            Text(text = "Hello this is Nearby Share Project", fontSize = 24.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Discover")
                //            Spacer(modifier = Modifier.width(1f.dp))
                Switch(checked = isDiscover, onCheckedChange = {
                    isDiscover = it
                    if (it) {
                        serviceManager.startDiscovery();
                    } else {
                        serviceManager.stopDiscovery()
                    }
                })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Advertising")
                //            Spacer(modifier = Modifier.fillMaxWidth())
                Switch(checked = isAdvertising, onCheckedChange = {
                    isAdvertising = it
                    if (it) {
                        serviceManager.startAdvertising()
                    } else {
                        serviceManager.stopAdvertising()
                    }
                })
            }
            if (reqConnection.isNotEmpty()) {
                Text(text = "Pending Connection")
                reqConnection.forEach { item ->
                    ListItem(
                        headlineContent = { Text(text = item.info?.endpointName!!) },
                        supportingContent = {
                            Text(
                                text = item.id
                            )
                        },
                        trailingContent = {
                            Row {
                                OutlinedButton(onClick = {
                                    serviceManager.accpetConnection(item.id)
                                }) {
                                    Text("Accept")
                                }
                                TextButton(onClick = {
                                    serviceManager.rejectConnection(item.id)
                                }) {
                                    Text(text = "Decline")
                                }
                            }
                        })
                }
            }
            if (discoverDevice.isNotEmpty()) {
                Text(text = "Endpoint Found")
                Column(modifier = Modifier.fillMaxSize()) {
                    // Menampilkan nilai dari dataListState dalam UI
                    discoverDevice.forEach { item ->
                        ListItem(headlineContent = {
                            Text(
                                text = item.endpointInfo?.endpointName ?: "Unknown"
                            )
                        }, supportingContent = {
                            Text(
                                text = item.id
                            )
                        }, modifier = Modifier.clickable {
                            serviceManager.requestConnect(item.id)
                        })
                    }
                }
            }

            if (connectedDevices.isNotEmpty()) {
                Text(text = "Connected Devices")
                connectedDevices.forEach { item ->
                    ListItem(
                        headlineContent = { Text(text = item.name) },
                        supportingContent = {
                            Text(
                                text = item.id
                            )
                        },
                        trailingContent = {
                            OutlinedButton(onClick = {
                                serviceManager.disconnectDevice(item.id)
                            }) {
                                Text(text = "Disconnect")
                            }
                        },
                        modifier = Modifier.clickable {
                            connectedTap(item)
                        })
                }
            }

        }
    }

//        if (devices.isNotEmpty()){
//
//
//
//        }else{
//            Text(text = "No Devices Found")
//        }
}
