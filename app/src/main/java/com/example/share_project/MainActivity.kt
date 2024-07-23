@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.share_project

//import NearbyServiceManager
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.share_project.View.ConnectedPage
import com.example.share_project.View.HomePage
import com.example.share_project.nearbyconnection.BatteryReceiver
import com.example.share_project.nearbyconnection.NearbyServiceManager
import com.example.share_project.nearbyconnection.tag
import com.example.share_project.ui.theme.Share_projectTheme
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Payload.File
import com.google.android.gms.nearby.connection.PayloadTransferUpdate


class MainActivity : ComponentActivity(), NearbyServiceManager.NearbyServiceListener,
    BatteryReceiver.BatteryListener {

    private val serviceManager = NearbyServiceManager(this)
    private var endpoints: MutableList<DevicesModel> = mutableListOf()

    private var discoverDevices by mutableStateOf<List<DevicesModel>>(listOf())
    private var conRequest by mutableStateOf<List<DeviceRequestModel>>(listOf())
    private var connectedEndpoint by mutableStateOf<List<DeviceConnectedModel>>(listOf())

    private var selectedDevice by mutableStateOf<DeviceConnectedModel?>(null)

    private var tempDevicedata by mutableStateOf<Map<String, Any>>(mapOf())

    private var sendedPayloadId by mutableStateOf<Long?>(null)
    private var transferUpdated by mutableStateOf<PayloadTransferUpdate?>(null)

    private var fileExchange by mutableStateOf<Map<String , List<File>>>(mapOf())

    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var notificationManager: NotificationManager


    override fun onSendedFile(endpointId: String, file: File) {

        super.onSendedFile(endpointId, file)
    }
    override fun onSendFile(endpointId: String, update: PayloadTransferUpdate) {
        Log.d(tag, "onSendFile: Call with update : ${update.payloadId}")
        Log.d(tag, "onSendFile: payload id is equal : ${update.payloadId == sendedPayloadId}")
        if(update.payloadId == sendedPayloadId){
        Log.d(tag, "onSendFile: Progress : ${(update.bytesTransferred.toFloat() / update.totalBytes.toFloat())}")

            transferUpdated = update
        }
        super.onSendFile(endpointId, update)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        batteryReceiver = BatteryReceiver()
        batteryReceiver.setListener(this)

        connectivityManager = getSystemService(ConnectivityManager::class.java)


        serviceManager.setListener(this)
        serviceManager.init()

        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ),
            2
        )
//        serviceManager.switchAdbWireless(this, this, true)
        setContent {

            Share_projectTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomePage(
                                serviceManager = serviceManager,
                                discoverDevices,
                                conRequest,
                                connectedEndpoint
                            ) { deviceConnectedModel ->
                                selectedDevice = deviceConnectedModel
                                batteryReceiver.register(this@MainActivity.baseContext)
//                            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                                notificationManager =
                                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                navController.navigate("connectedpage")
                            }
                        }
                        composable("connectedpage") {
                            ConnectedPage(
                                activity = this@MainActivity,
                                deviceConnectedModel = selectedDevice,
                                sendedPayloadId,
                                transferUpdated,
                                onSelectedFile = {
                                    if (selectedDevice != null) {
                                        sendedPayloadId = serviceManager.sendFile(selectedDevice!!.id, it)
                                    }
                                }
                            )
                        }
                    }
                    if (selectedDevice == null && navController.currentDestination?.route == "connectedpage") {
                        navController.navigate("home")
                    }
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.e(
                com.example.share_project.nearbyconnection.tag,
                "The default network is now: " + network
            )
        }

        override fun onLost(network: Network) {
            Log.e(
                com.example.share_project.nearbyconnection.tag,
                "The application no longer has a default network. The last default network was " + network
            )
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (selectedDevice != null) {
                val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
                val rssi = wifiManager.connectionInfo.rssi
                val level = WifiManager.calculateSignalLevel(rssi, 10)

                serviceManager.sendSignalLengthPhone(selectedDevice!!.id, level)
            }
            Log.e(
                com.example.share_project.nearbyconnection.tag,
                "The default network changed capabilities: " + networkCapabilities.signalStrength
            )
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            Log.e(
                com.example.share_project.nearbyconnection.tag,
                "The default network changed link properties: " + linkProperties
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceManager.stopAdvertising()
//        serviceManager.switchAdbWireless(this, this, false)
        serviceManager.stopDiscovery()
        unregisterReceiver(batteryReceiver)
    }

    private fun updateDataList(newList: List<DevicesModel>) {
        discoverDevices = newList
    }

    override fun onEndpointFound(id: String, endpointInfo: DiscoveredEndpointInfo) {
        Log.d(tag, "onEndpointFound: in mainactivity")
        if (!endpoints.any { it.endpointInfo?.endpointName == endpointInfo.endpointName }) {

            endpoints.add(DevicesModel(id, endpointInfo))
            endpoints.add(DevicesModel(id, endpointInfo))
        }
        updateDataList(discoverDevices + DevicesModel(id, endpointInfo))
        Log.d(tag, "datalistState : ${discoverDevices}")
    }

    override fun onEndpointLost(id: String) {
        endpoints.removeIf { it.id == id }
        updateDataList(endpoints)
        Log.d(tag, "discover devices : ${discoverDevices}")

    }

    override fun onConnectionAccepted(id: String, result: ConnectionResolution?) {

        Log.d(tag, "onConnectionAccepted: Success")
        val temp = mutableListOf<DeviceRequestModel>()
        temp.addAll(conRequest)
        val dataConnected = temp.find { it.id == id }
        if (dataConnected != null) {

            val newData = DeviceConnectedModel(id, dataConnected!!.info.endpointName)
            temp.remove(dataConnected)
            conRequest = temp
            connectedEndpoint += newData
            Log.d(tag, "onConnectionAccepted: ${connectedEndpoint}")
        }

    }

    override fun onConnectionRequest(id: String, info: ConnectionInfo) {
        conRequest += DeviceRequestModel(id, info)
    }

    override fun onConnectionRejected(id: String) {
        Log.d(tag, "onConnectionRejected: Reject")
    }

    override fun onConnectionError(id: String) {
        Log.d(tag, "onConnectionError: Error on ${id}")
    }

    override fun onStopDiscover() {
        Log.d(tag, "onStopDiscover: Success")
        discoverDevices = listOf()
    }

    override fun onStopAdvertising() {
        Log.d(tag, "onStopAdvertising: w")
//        conRequest = listOf()
//        connectedEndpoint = listOf()
    }

    override fun onPayloadReceive(endpointId: String, payload: Payload) {

    }

    override fun onDeviceDisconnect(endpointId: String) {
        selectedDevice = null
//        var newList = listOf()
//        connectedEndpoint.removeIf { it.id == endpointId }
        val temp = mutableListOf<DeviceConnectedModel>()
        temp.addAll(connectedEndpoint)
        temp.removeIf { it.id == endpointId }
        connectedEndpoint = temp
        batteryReceiver.unregister(this.baseContext)
    }

    override fun onBatteryChanged(level: Int, isCharging: Boolean) {
        if (selectedDevice != null) {
            if (tempDevicedata["battery_level"] != level || tempDevicedata["isCharging"] != isCharging) {
                Log.d(
                    com.example.share_project.nearbyconnection.tag,
                    "onBatteryChanged: Update send"
                )
                serviceManager.sendDataPhone(selectedDevice!!.id, level, isCharging)
                tempDevicedata = mapOf(
                    "battery_level" to level,
                    "isCharging" to isCharging
                )
            } else {
                Log.d(
                    com.example.share_project.nearbyconnection.tag,
                    "onBatteryChanged: Not Update"
                )
            }
        }

    }

//    override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
//        if(!devices.any { it.serviceName == serviceInfo?.serviceName }){
//            if (serviceInfo != null) {
//
//                devices.add(serviceInfo)
//            }
//        }
//    }

}


