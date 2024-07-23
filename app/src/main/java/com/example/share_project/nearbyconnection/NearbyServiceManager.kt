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
import androidx.collection.SimpleArrayMap
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
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets


const val SERVICE_ID = "NearbySharing"
const val tag = "NearbyServiceManager"

class NearbyServiceManager(private val context: Context) {

    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads = SimpleArrayMap<Long, Payload>()
    private val filePayloadFilenames = SimpleArrayMap<Long, String>()
     lateinit var connectionClient: ConnectionsClient ;

//    private lateinit var batteryReceiver: BatteryReceiver
    private val payloadCallback = object : PayloadCallback(){
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Log.d(tag, "onPayloadReceived: as " + payload.type)
            if (payload.type == Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                //            long payloadId = addPayloadFilename(payloadFilenameMessage);
//            processFilePayload(payloadId);
                Log.d(tag, "onPayloadReceived: as Byte$payloadFilenameMessage")
            } else if (payload.type == Payload.Type.FILE) {
                // Add this to our tracking map, so that we can retrieve the payload later.
//            String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);
//            long payloadId = addPayloadFilename(payloadFilenameMessage);
//            processFilePayload(payloadId);
                incomingFilePayloads.put(payload.id, payload)
                Log.d(tag, "onPayloadReceived: as FILE")
            }
        }

        override fun onPayloadTransferUpdate(p0: String, update: PayloadTransferUpdate) {
            Log.d(
                tag,
                "onPayloadTransferUpdate: Update getBytesTransferred : " + update.getBytesTransferred()
            )
            Log.d(tag, "onPayloadTransferUpdate: Update getTotalBytes : " + update.getTotalBytes())
            listener?.onSendFile(p0, update)
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId: Long = update.getPayloadId()
                val payload = incomingFilePayloads.remove(payloadId)
                if (payload != null) {
                    if (payload.type == Payload.Type.FILE) {
                        completedFilePayloads.put(payloadId, payload)
                        Log.d(tag, "onPayloadTransferUpdate: FIle telah diterima")
                        processFilePayload(payloadId, p0)
                    }
                }
            } else if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {
            }
        }
    }
//    private val payloadCallback = ReceiveFilePayloadCallback(context)

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

    private fun addPayloadFilename(payloadFilenameMessage: String): Long {
        val parts = payloadFilenameMessage.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val payloadId = parts[0].toLong()
        val filename = parts[1]
        filePayloadFilenames.put(payloadId, filename)
        return payloadId
    }

    private fun processFilePayload(payloadId: Long, endpointId: String) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        val filePayload = completedFilePayloads[payloadId]
        val filename = filePayloadFilenames[payloadId]
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId)
            filePayloadFilenames.remove(payloadId)

            // Get the received file (which will be in the Downloads folder)
            // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
            // allowed to access filepaths from another process directly. Instead, we must open the
            // uri using our ContentResolver.
            val uri = filePayload.asFile()!!.asUri()
            try {
                // Copy the file to a new location.
                val `in` = context.contentResolver.openInputStream(uri!!)
                copyStream(`in`, FileOutputStream(File(context.cacheDir, filename)))
            } catch (e: IOException) {
                // Log the error.
            } finally {
                listener?.onSendedFile(endpointId= endpointId, file= filePayload.asFile()!!)
                // Delete the original file.
                context.contentResolver.delete(uri!!, null, null)
            }
        }
    }

    // add removed tag back to fix b/183037922
    private fun processFilePayload2(payloadId: Long) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        val filePayload = completedFilePayloads[payloadId]
        val filename = filePayloadFilenames[payloadId]
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId)
            filePayloadFilenames.remove(payloadId)

            // Get the received file (which will be in the Downloads folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                val uri = filePayload.asFile()!!.asUri()
                try {
                    // Copy the file to a new location.
                    val `in` = context.contentResolver.openInputStream(uri!!)
                    copyStream(`in`, FileOutputStream(File(context.cacheDir, filename)))
                } catch (e: IOException) {
                    // Log the error.
                } finally {
                    // Delete the original file.
                    context.contentResolver.delete(uri!!, null, null)
                }
            } else {
                val payloadFile = filePayload.asFile()!!.asJavaFile()

                // Rename the file.
                payloadFile!!.renameTo(File(payloadFile!!.parentFile, filename))
            }
        }
    }


    /** Copies a stream from one location to another.  */
    @Throws(IOException::class)
    private fun copyStream(`in`: InputStream?, out: OutputStream) {
        try {
            val buffer = ByteArray(1024)
            var read: Int
            while ((`in`!!.read(buffer).also { read = it }) != -1) {
                out.write(buffer, 0, read)
            }
            out.flush()
        } finally {
            `in`!!.close()
            out.close()
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
        }.addOnFailureListener {
            Log.d(tag, "startDiscovery: Failed : ${it.toString()}") };
    }


    fun stopDiscovery(){
        listener?.onStopDiscover()
        connectionClient.stopDiscovery();
        Log.d(tag, "stopDiscovery: Succcess")

    }

    fun sendSignalLengthPhone(endpointId: String, signalLength: Int){
//        WifiManager.calculateSignalLevel()
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

    fun sendFile(endpointId: String, file: File): Long{
        val payload = Payload.fromFile(file)
        payload.setFileName(file.name)
        Log.d(tag, "sendFile: Payload $payload")
        connectionClient.sendPayload(endpointId, payload)
        return payload.id
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

    fun disconnectDevice(id: String){
        connectionClient.disconnectFromEndpoint(id)
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
        fun onSendFile(endpointId: String, update: PayloadTransferUpdate){}
        fun onSendedFile(endpointId: String, file: com.google.android.gms.nearby.connection.Payload.File){}
    }

    private var listener : NearbyServiceListener? = null;

    fun setListener(listener: NearbyServiceListener){
        this.listener = listener
        Log.d(tag, "setListener: Has Set")
    }



}