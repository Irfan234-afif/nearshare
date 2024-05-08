package com.example.share_project

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo

data class DevicesModel(
    val id:String, val endpointInfo: DiscoveredEndpointInfo? = null
)

data class DeviceRequestModel(
    val id:String, val info: ConnectionInfo
)

data class DeviceConnectedModel(
    val id:String, val name: String
)