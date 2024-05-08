package com.example.share_project.nearbyconnection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.util.Log

class BatteryReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(Intent.ACTION_BATTERY_CHANGED == intent?.action){
            val level : Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
            listener?.onBatteryChanged(level, isCharging)
            Log.d(tag, "onReceive: Battery Level : $level , isCharge : $isCharging")
        }
//        if(Intent.)
    }

    interface BatteryListener{
        fun onBatteryChanged(level: Int, isCharging : Boolean)
    }
    private var listener: BatteryListener? = null

    fun setListener(listener: BatteryListener){
        this.listener = listener;
    }
}