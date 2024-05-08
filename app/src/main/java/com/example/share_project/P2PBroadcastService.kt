package com.example.share_project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log

class P2PBroadcastService(
    private val p2pManager: WifiP2pManager,
    private val p2pChannel: Channel,
) : BroadcastReceiver() {

    var peers: MutableList<String> = mutableListOf()
    var connectedDevice: WifiP2pDevice? = null
    var currentDevice: WifiP2pDevice? = null
    var wifiInfo: WifiP2pInfo? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("BroadcastReceive", "state changed: changed")
        when (intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                Log.d("BroadcastReceive", "state changed: changed")
                p2pManager.requestConnectionInfo(p2pChannel){info ->
                    Log.d("BroadcastReceive", "INFO: $info")
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                Log.d("BroadcastReceive", "onReceive: changed")
                try {
                    p2pManager.requestPeers(p2pChannel) { newPeers: WifiP2pDeviceList? ->
                        // Handle peers list
                        Log.d("BroadcastReceive", "onReceive: ${newPeers?.deviceList}")
                        val list: MutableList<String> = mutableListOf()

                        if (newPeers != null) {
                            if (newPeers.deviceList.isEmpty() && connectedDevice != null) {
                                connectedDevice = null
                            }
                        }
                        if (newPeers != null) {
                            for (device: WifiP2pDevice in newPeers.deviceList) {
//                                list.add(device.toJsonString())
                                Log.d("BroadcastReceive", "onReceive: $list")
                                if (device.status == WifiP2pDevice.CONNECTED) {
                                    connectedDevice = device
                                } else if (device.deviceAddress == connectedDevice?.deviceAddress &&
                                    device.status != WifiP2pDevice.CONNECTED
                                ) {
                                    connectedDevice = null
                                }
                            }
                        }
                        peers = list
                    }
                }catch (e: SecurityException){
                    Log.d("BroadcastReceive", "Security Permission Exception: ${e.toString()}")
                }

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
            }
        }
    }

}