package com.example.share_project
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import androidx.compose.material3.AlertDialogDefaults

class ServiceBrowser(private val context: Context) {

    private val manager: WifiP2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    private val channel: Channel by lazy {
        manager.initialize(context, context.mainLooper, null)
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("ServiceBrowser", "onReceive: Founding Device")
            val action: String? = intent.action
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
                // Mencari daftar perangkat Wi-Fi P2P yang tersedia
                Log.d("ServiceBrowser", "onReceive: Founding Device")
                try {
                    manager.requestPeers(channel) { peers: WifiP2pDeviceList ->
                        // Handle daftar perangkat yang ditemukan
                        Log.d("ServiceBrowser", "onReceive: Found Device ${peers.toString()}")
                        val deviceList: List<WifiP2pDevice> = peers.deviceList.toList()
                        for (device in deviceList) {
                            // Periksa apakah perangkat sesuai dengan kriteria layanan yang diinginkan
                            if (device.deviceName == "NamaPerangkatMacOS" && device.isServiceDiscoveryCapable) {
                                // Lakukan sesuatu dengan perangkat yang ditemukan
                            }
                        }
                    }
                }catch (e: SecurityException){
                    Log.d("ServiceBrowser", "onReceive: Permission Required")
                }

            }
        }
    }

    fun startDiscovery() {
        Log.d("ServiceBrowser", "startDiscovery")
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        context.registerReceiver(receiver, intentFilter)

        try {
            manager.discoverPeers(channel, object : ActionListener {
                override fun onSuccess() {
                    // Discovery berhasil dimulai
                    Log.d("ServiceBrowser", "startDiscovery: Success")
                }

                override fun onFailure(reason: Int) {
                    // Gagal memulai discovery, handle kesalahan
                }
            })
        }catch (e: SecurityException){
            Log.d("ServiceBrowser", "startDiscovery: Permission Required")
        }

    }

    fun stopDiscovery() {
        context.unregisterReceiver(receiver)
        manager.stopPeerDiscovery(channel, null)
    }
}
