package com.example.share_project.nearbyconnection

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.share_project.MainActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson


const val SERVICE_ID = "NearbySharing"
const val tag = "NearbyServiceManager"

class NearbyServiceManager(private val context: Context) {
     lateinit var connectionClient: ConnectionsClient ;

//    private lateinit var batteryReceiver: BatteryReceiver
//    private val payloadCallback = object : PayloadCallback(){
//        override fun onPayloadReceived(endpointId: String, payload: Payload) {
//            Log.d(tag, "onPayloadReceived: $endpointId : ${payload}")
//            when(payload.type){
//                Payload.Type.BYTES ->{
//                    val data = java.lang.String(payload.asBytes())
//                    Log.d(tag, "onPayloadReceived: Result : $data")
//                }
//                Payload.Type.FILE -> {
//
//                }
//            }
//        }
//
//        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
//            Log.d(tag, "onPayloadTransferUpdate: $p0 : ${p1.toString()}")
//        }
//    }
    private val payloadCallback = ReceiveFilePayloadCallback(context)

    fun init() {
        connectionClient = Nearby.getConnectionsClient(context)

//        batteryReceiver= BatteryReceiver()
    }

    private val conLifeCycle :ConnectionLifecycleCallback= object  : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(tag, "onConnectionInitiated: ${endpointId} : ${info.endpointName}");
            listener?.onConnectionRequest(endpointId, info);
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            Log.d(tag, "onConnectionResult: $p0 : $p1.");
            Log.d(tag, "onConnectionResult: code ${p1.status.statusCode}");
            when(p1.status.statusCode){
//                ConnectionsStatusCodes.
                ConnectionsStatusCodes.STATUS_OK ->{
                    listener?.onConnectionAccepted(p0, p1)
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> listener?.onConnectionRejected(p0)
                ConnectionsStatusCodes.STATUS_ERROR -> listener?.onConnectionError(p0)
                else -> listener?.onConnectionError(p0)
            }
            Log.d(tag, "onConnectionResult: $p0 : $p1");
        }

        override fun onDisconnected(p0: String) {
            Log.d(tag, "onDisconnected: $p0")
            listener?.onDeviceDisconnect(p0)
        }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object :EndpointDiscoveryCallback(){
        override fun onEndpointFound(ser: String, endpointInfo: DiscoveredEndpointInfo) {
            Log.d(tag, "onEndpointFound: Found Endpoint ${ser} : ${endpointInfo.toString()}")
            listener?.onEndpointFound(ser, endpointInfo)
        }

        override fun onEndpointLost(p0: String) {
            Log.d(tag, "onEndpointLost: Endpoint Lost ${p0}");
            listener?.onEndpointLost(p0)
        }
    }


    fun requestPermission(activity: Activity){
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), 1)
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
    }
    fun startAdvertising(){
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        connectionClient.startAdvertising(Build.MODEL, SERVICE_ID, conLifeCycle, options).addOnSuccessListener {
            Log.d(tag, "startAdvertising: Success");
        }.addOnFailureListener { error : Exception ->
            Log.d(tag, "startAdvertising: Error on ${error.toString()}")
        }
    }

    fun startDiscovery(){
        val options:DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        connectionClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback,options).addOnSuccessListener {
            Log.d(tag, "startDiscovery: Success")
        }.addOnFailureListener { Log.d(tag, "startDiscovery: Failed") };
    }

    fun stopDiscovery(){
        listener?.onStopDiscover()
        connectionClient.stopDiscovery();
        Log.d(tag, "stopDiscovery: Succcess")

    }

    fun sendSignalLengthPhone(endpointId: String, signalLength: Int){
        val dataToSend = mapOf<String, Any>(
            "kind" to "update",
            "signalLength" to signalLength
        )

        val parseToJson = Gson().toJson(dataToSend)
        val payload = Payload.fromBytes(parseToJson.toByteArray())
        connectionClient.sendPayload(endpointId, payload)
    }

     fun sendDataPhone(endpointId: String, batteryLevel: Int, isCharging : Boolean){
        val dataToSend = mapOf<String, Any>(
            "kind" to "initial",
            "battery_level" to batteryLevel.toString(),
            "isCharging" to isCharging,
            "endpointName" to Build.MODEL
        )
         val parseToJson = Gson().toJson(dataToSend)
        val payload = Payload.fromBytes(parseToJson.toByteArray())
        connectionClient.sendPayload(endpointId, payload)
    }

    fun getBatteryStatus() : Int{
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        Log.d(tag, "getBatteryStatus: Status : $status")
        return status;

    }

    fun stopAdvertising(){
        listener?.onStopAdvertising()
        connectionClient.stopAdvertising();
        Log.d(tag, "stopAdvertising: Success")

    }

    fun  requestConnect(id : String) : Boolean{
        var result = false;
        connectionClient.requestConnection("Android", id,conLifeCycle).addOnSuccessListener {
            Log.d(tag, "requestConnect: Success")
            result = true;
        }.addOnFailureListener {
            Log.d(tag, "requestConnect: Failed")
            result = false;
        }
        return result
    }

    fun accpetConnection(endpointId: String){
        connectionClient.acceptConnection(endpointId, payloadCallback).addOnSuccessListener {
            listener?.onConnectionAccepted(endpointId)
        }.addOnFailureListener {
            listener?.onConnectionError(endpointId)
        }
    }

    fun rejectConnection(endpointId: String){
        connectionClient.rejectConnection(endpointId)
    }

    fun switchAdbWireless(context: Context, activity: MainActivity, isActive: Boolean){
        val enableAdb = Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", if(isActive) 1 else 0)
        Log.d(tag, "switchAdbWireless: $enableAdb")
    }

    interface NearbyServiceListener{
        fun onEndpointFound(id:String, endpointInfo:DiscoveredEndpointInfo)
        fun onEndpointLost(id: String)
        fun onConnectionRequest(id: String, info: ConnectionInfo)
        fun onConnectionAccepted(id: String, result: ConnectionResolution? = null)
        fun onConnectionRejected(id: String)
        fun onConnectionError(id: String)
        fun onStopDiscover()
        fun onStopAdvertising()
        fun onPayloadReceive(endpointId: String, payload: Payload)
        fun onDeviceDisconnect(endpointId: String)
    }

    private var listener : NearbyServiceListener? = null;

    fun setListener(listener: NearbyServiceListener){
        this.listener = listener
        Log.d(tag, "setListener: Has Set")
    }



}