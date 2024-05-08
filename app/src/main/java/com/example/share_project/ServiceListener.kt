package com.example.share_project

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

const val tag = "Service Listener"


class ServiceListener : NsdManager.DiscoveryListener {
    override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
//        TODO("Not yet implemented")
    }

    override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
//        TODO("Not yet implemented")
    }

    override fun onDiscoveryStarted(serviceType: String?) {
        Log.d(tag, "onDiscoveryStarted: Starting")
    }

    override fun onDiscoveryStopped(serviceType: String?) {
//        TODO("Not yet implemented")
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
        Log.d(tag, "onServiceFound: ${serviceInfo?.toString()}")
        listener?.onServiceFound(serviceInfo)
//        TODO("Not yet implemented")

    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
//        TODO("Not yet implemented")
    }

    interface NSDListener {
        fun onServiceFound(serviceInfo: NsdServiceInfo?)
    }

    private var listener : NSDListener? = null;

    fun setListener(listener: NSDListener){
        this.listener = listener
        Log.d(tag, "setListener: has set")
    }
}