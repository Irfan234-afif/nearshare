package com.example.share_project.nearbyconnection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log


class BatteryReceiver: BroadcastReceiver() {
    private var isRegistered: Boolean = false

    /**
     * register receiver
     * @param context - Context
     * @param filter - Intent Filter
     * @return see Context.registerReceiver(BroadcastReceiver,IntentFilter)
     */
    fun register(context: Context): Intent? {
        try {
            // ceph3us note:
            // here I propose to create
            // a isRegistered(Contex) method
            // as you can register receiver on different context
            // so you need to match against the same one :)
            // example  by storing a list of weak references
            // see LoadedApk.class - receiver dispatcher
            // its and ArrayMap there for example
            return if (!isRegistered
            ) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(this, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } else {
                null
            }
            else null
        } finally {
            isRegistered = true
        }
    }

    /**
     * unregister received
     * @param context - context
     * @return true if was registered else false
     */
    fun unregister(context: Context): Boolean {
        // additional work match on context before unregister
        // eg store weak ref in register then compare in unregister
        // if match same instance
        return (isRegistered
                && unregisterInternal(context))
    }

    private fun unregisterInternal(context: Context): Boolean {
        context.unregisterReceiver(this)
        isRegistered = false
        return true
    }


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