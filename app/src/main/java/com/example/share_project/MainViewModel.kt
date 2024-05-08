package com.example.share_project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    private var _endpoints = MutableStateFlow<List<DevicesModel>>(listOf())
    val endpoints = _endpoints.asStateFlow()

    fun addEndpoint(newEndpoints: List<DevicesModel>){
//       _endpoints.value.addAll(endpoints)
        _endpoints.value = newEndpoints;
    }
}