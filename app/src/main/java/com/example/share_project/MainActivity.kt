@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.share_project

//import NearbyServiceManager
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.share_project.nearbyconnection.BatteryReceiver
import com.example.share_project.nearbyconnection.NearbyServiceManager
import com.example.share_project.ui.theme.Share_projectTheme
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.Payload

class MainActivity : ComponentActivity(), NearbyServiceManager.NearbyServiceListener, BatteryReceiver.BatteryListener {
    private val serviceManager = NearbyServiceManager(this)
    private var endpoints:MutableList<DevicesModel> = mutableListOf()

    private var discoverDevices by mutableStateOf<List<DevicesModel>>(listOf())
    private var conRequest by mutableStateOf<List<DeviceRequestModel>>(listOf())
    private var connectedEndpoint by mutableStateOf<List<DeviceConnectedModel>>(listOf())

    private var selectedDevice by mutableStateOf<DeviceConnectedModel?>(null)

    private var tempDevicedata by mutableStateOf<Map<String, Any>>(mapOf())

    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var  connectivityManager : ConnectivityManager
    private lateinit var notificationManager: NotificationManager


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        batteryReceiver = BatteryReceiver()
        batteryReceiver.setListener(this)

        connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        serviceManager.setListener(this)
        serviceManager.init()

        serviceManager.switchAdbWireless(this, this, true)
        setContent {

            Share_projectTheme {

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val navController = rememberNavController()
                    NavHost(navController = navController,startDestination = "home"){
                        composable("home"){HomePage(serviceManager = serviceManager,discoverDevices,conRequest,connectedEndpoint){deviceConnectedModel ->
                            selectedDevice = deviceConnectedModel
                            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            navController.navigate("connectedpage")
                        } }
                        composable("connectedpage") { ConnectedPage(activity = this@MainActivity,deviceConnectedModel = selectedDevice)}
                    }
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback(){
        override fun onAvailable(network : Network) {
            Log.e(com.example.share_project.nearbyconnection.tag, "The default network is now: " + network)
        }

        override fun onLost(network : Network) {
            Log.e(com.example.share_project.nearbyconnection.tag, "The application no longer has a default network. The last default network was " + network)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
            if(selectedDevice != null){
            serviceManager.sendSignalLengthPhone(selectedDevice!!.id, networkCapabilities.signalStrength)
            }
            Log.e(com.example.share_project.nearbyconnection.tag, "The default network changed capabilities: " + networkCapabilities)
        }

        override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
            Log.e(com.example.share_project.nearbyconnection.tag, "The default network changed link properties: " + linkProperties)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceManager.stopAdvertising()
        serviceManager.switchAdbWireless(this, this, false)
        serviceManager.stopDiscovery()
        unregisterReceiver(batteryReceiver)
    }

    private fun updateDataList(newList: List<DevicesModel>) {
        discoverDevices = newList
    }

    override fun onEndpointFound(id: String, endpointInfo: DiscoveredEndpointInfo) {
        Log.d(tag, "onEndpointFound: in mainactivity")
        if(!endpoints.any { it.endpointInfo?.endpointName == endpointInfo.endpointName }){

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

    override fun onConnectionAccepted(id: String, result:ConnectionResolution?) {

        Log.d(tag, "onConnectionAccepted: Success")
        val temp = mutableListOf<DeviceRequestModel>()
        temp.addAll(conRequest)
        val dataConnected = temp.find { it.id == id}
        if(dataConnected != null){

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
        val temp = mutableListOf<DeviceConnectedModel>()
        temp.addAll(connectedEndpoint)
        temp.removeAll { it.id == endpointId }
        connectedEndpoint = temp
        unregisterReceiver(batteryReceiver)
    }

    override fun onBatteryChanged(level: Int, isCharging : Boolean) {
        if(selectedDevice != null){
            if(tempDevicedata["battery_level"] != level || tempDevicedata["isCharging"] != isCharging){
                Log.d(com.example.share_project.nearbyconnection.tag, "onBatteryChanged: Update send")
                serviceManager.sendDataPhone(selectedDevice!!.id, level, isCharging)
                tempDevicedata = mapOf(
                    "battery_level" to level,
                    "isCharging" to isCharging
                )
            }else{
                Log.d(com.example.share_project.nearbyconnection.tag, "onBatteryChanged: Not Update")
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

@Composable
fun MyApp(dataListState: List<DevicesModel>, serviceManager: NearbyServiceManager) {

    Column(modifier = Modifier.fillMaxSize()) {
        // Menampilkan nilai dari dataListState dalam UI
        dataListState.forEach { item ->
//            Text(text = "- ${item.endpointInfo?.endpointName}")
            androidx.compose.material3.ListItem(headlineContent = { Text(text = item.endpointInfo?.endpointName ?: "Unknown")}, supportingContent = { Text(
                text = item.id
            )}, modifier = Modifier.clickable {
//                onTapDevice(item)
                serviceManager.requestConnect(item.id)
            })
        }

        // Tombol untuk memperbarui nilai dataListState
//        Button(onClick = { updateDataList(dataListState + DevicesModel(id = "new")) }) {
//            Text(text = "Add New Item")
//        }
//        Button(onClick = { updateDataList(dataListState - DevicesModel(id = "new")) }) {
//            Text(text = "Remove New Item")
//        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomePage(
    serviceManager: NearbyServiceManager,
    discoverDevice: List<DevicesModel> = listOf(),
    reqConnection:List<DeviceRequestModel> = listOf(),
    connectedDevices: List<DeviceConnectedModel> = listOf(),
    connectedTap: (DeviceConnectedModel) -> Unit
){
    var isDiscover by remember { mutableStateOf(false) }
    var isAdvertising by remember { mutableStateOf(false) }
    Scaffold (
        topBar = {
            TopAppBar(title = { Text(text = "Nearby Connect")}, colors = topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),)
        }
    ){innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp), Arrangement.Top, Alignment.Start,
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
                        if(it){
                            serviceManager.startDiscovery();
                        }else{
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
                        if(it){
                            serviceManager.startAdvertising()
                        }else{
                            serviceManager.stopAdvertising()
                        }
                    })
                }
                if(reqConnection.isNotEmpty()){
                    Text(text = "Pending Connection")
                    reqConnection.forEach{item ->
                        androidx.compose.material3.ListItem(headlineContent = { Text(text = item.info?.endpointName!!)}, supportingContent = { Text(
                            text = item.id
                        )}, trailingContent = { Row {
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
                        }})
                    }
                }
                if(discoverDevice.isNotEmpty()){
                    Text(text = "Endpoint Found")
                    MyApp(discoverDevice, serviceManager)    
                }
                
                if(connectedDevices.isNotEmpty()){
                    Log.d(tag, "BodyApp: Connected Devices ${connectedDevices}")
                    Text(text = "Connected Devices")
                    connectedDevices.forEach { item ->
                        androidx.compose.material3.ListItem(headlineContent = { Text(text = item.name)}, supportingContent = { Text(
                            text = item.id
                        )}, trailingContent = { OutlinedButton(onClick = {}) {
                            Text(text = "Send")
                        }}, modifier = Modifier.clickable {
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

//}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    Share_projectTheme {
//        Greeting()
//    }
//}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedPage(activity: MainActivity,deviceConnectedModel: DeviceConnectedModel?){
    if(deviceConnectedModel != null){


    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(text = "Communicate")
            })
        }
    ) { innerPadding ->
        Column (modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)){
            androidx.compose.material3.ListItem(headlineContent = { Text(text = deviceConnectedModel.name)})
            Button(onClick = {
                val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(activity,Intent.createChooser(intent, "Select a file"), 2, Bundle())

            }) {
                Text(text = "Choose File")
            }
        }

    }
    }
}