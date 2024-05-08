package com.example.share_project

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService

const val SERVICE_TYPE = "_FC9F5ED42C8A._tcp"

class P2PService {
    private lateinit var nsdManager : NsdManager;
    lateinit var nsdListener : ServiceListener;
    private lateinit var p2pManager : WifiP2pManager;
    private lateinit var p2pChannel : WifiP2pManager.Channel ;
    private lateinit var p2pConfig: WifiP2pConfig;
    private lateinit var peerListener: WifiP2pManager.PeerListListener;
    private lateinit var receiver: P2PBroadcastService;

    private val intentFilter = IntentFilter()
    private val buddies = mutableMapOf<String, String>()

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e("Service Listener", "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e("Service Listener", "Resolve Succeeded. $serviceInfo")

//            if (serviceInfo.serviceName == mServiceName) {
//                Log.d("Service Listener", "Same IP.")
//                return
//            }
//            mService = serviceInfo
//            val port: Int = serviceInfo.port
//            val host: InetAddress = serviceInfo.host
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d("Service Listener", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d("Service Listener", "Service discovery success$service")
            nsdManager.resolveService(service, resolveListener)
//            when {
//                service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
//                    // transport layer for this service.
//                    Log.d("Service Listener", "Unknown Service Type: ${service.serviceType}")
//                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
//                    // connecting to. It could be "Bob's Chat App".
//                    Log.d("Service Listener", "Same machine: $mServiceName")
//                service.serviceName.contains("NsdChat") -> nsdManager.resolveService(service, resolveListener)
//            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e("Service Listener", "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("Service Listener", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Service Listener", "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Service Listener", "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun init(context: Context, listener: ServiceListener){
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
//        nsdListener = ServiceListener();
        nsdListener = listener
        p2pManager =context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        p2pChannel = p2pManager.initialize(context, context.mainLooper, null)
        p2pConfig = WifiP2pConfig();
        peerListener = WifiP2pManager.PeerListListener { peers: WifiP2pDeviceList? ->
            if(peers != null && peers.deviceList.isNotEmpty()){
                Log.d(tag, "PeerListListener: ${peers.deviceList.first().deviceAddress}");
            }

        }
        val record: Map<String, String> = mapOf(
            "buddyname" to "John Doe${(Math.random() * 1000).toInt()}",
            "available" to "visible"
        )
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("Android", "_FC9F5ED42C8A._tcp", record)
        try {
            p2pManager.addLocalService(p2pChannel,serviceInfo, object : WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Log.d("HALO", "onSuccess: AddLocalService Succcess");
                }

                override fun onFailure(reason: Int) {
                    Log.d("HALO", "onFailure: AddLocalService Failed")
                }

            })
        }catch (e: SecurityException){
            Log.d("HALO", "init: Permission Error");
        }

        addWifiActions()
        initReceiver(context = context)

    }

    private fun addWifiActions() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
    }

    private fun initReceiver(context: Context){

        receiver=P2PBroadcastService(p2pManager, p2pChannel);
        context.registerReceiver(receiver, intentFilter)
    }

    fun startDiscovery(context: Context){
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, nsdListener);
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d("DiscoveryP2P", "startDiscovery: Failed Permission")
            return
        }
        Log.d("DiscoveryP2P", "startDiscovery: Success Permission")
        p2pManager.discoverServices(p2pChannel, object : WifiP2pManager.ActionListener{

            override fun onSuccess() {
                Log.d("DiscoveryP2P", "DiscoverService Success: ")
            }

            override fun onFailure(reason: Int) {
                Log.d("DiscoveryP2P", "DiscoverService Failed: ")
            }

        })
        p2pManager.discoverPeers(p2pChannel, object : WifiP2pManager.ActionListener{
            override fun onSuccess() {
                Log.d("DiscoveryP2P", "DiscoverPeers Success: ")
            }

            override fun onFailure(reason: Int) {
                Log.d("DiscoveryP2P", "DiscoverPeers Failed: ")
            }

        })
    }

     fun directDiscovery(){
        val servListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName =
                    buddies[resourceType.deviceAddress] ?: resourceType.deviceName

                // Add to the custom adapter defined specifically for showing
                // wifi devices.
//                FragmentManage
//                val fragment = fragmentManager
//                    .findFragmentById(R.id.frag_peerlist) as WiFiDirectServicesList
//                (fragment.listAdapter as WiFiDevicesAdapter).apply {
//                    add(resourceType)
//                    notifyDataSetChanged()
//                }

                Log.d("HALO", "onBonjourServiceAvailable $instanceName")
            }

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener{fullDomainName, txtRecordMap, srcDevice ->
            Log.d("HALO", "DnsSdTxtRecord available -$txtRecordMap")
            txtRecordMap["buddyname"]?.also {
                buddies[srcDevice.deviceAddress] = it
            }
        };
        p2pManager.setDnsSdResponseListeners(p2pChannel, servListener, txtListener)
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()

        p2pManager.addServiceRequest(
            p2pChannel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Success!
                    Log.d("HALO", "onSuccess: Action Listener")
                }

                override fun onFailure(code: Int) {
                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    Log.d("HALo", "onFailure: ActionListener Failed on $code")
                }
            }
        )
         try {
             p2pManager.discoverServices(
                 p2pChannel,
                 object : WifiP2pManager.ActionListener {
                     override fun onSuccess() {
                         // Success!
                         Log.d("HALO", "onSuccess: DiscoverService Success")
                     }

                     override fun onFailure(code: Int) {
                         // Command failed. Check for P2P_UNSUPPORTED, ERROR, or BUSY
                         when (code) {
                             WifiP2pManager.P2P_UNSUPPORTED -> {
                                 Log.d("HALO", "Wi-Fi Direct isn't supported on this device.")
                             }
                         }
                     }
                 }
             )
         }catch (e: SecurityException){}

    }

    fun connect(serviceInfo: NsdServiceInfo) {
//        var deviceTarget = WifiP2pDevice.;
        try {
            p2pManager.connect(p2pChannel, p2pConfig, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
//                    TODO("Not yet implemented")
                    Log.d("ConnectPeer", "onSuccess: Succes Connect")
                }

                override fun onFailure(reason: Int) {
//                    TODO("Not yet implemented")
                    Log.d("ConnectPeer", "onFailure: Failed ${reason.toString()}")
                }
            })
        }catch (e: SecurityException){
            Log.d("ConnectPeer", "connect: Failed ${e.toString()}")
        }
    }

}