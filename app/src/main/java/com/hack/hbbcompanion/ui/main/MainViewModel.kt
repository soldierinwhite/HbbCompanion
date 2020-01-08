package com.hack.hbbcompanion.ui.main

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.hack.hbbcompanion.DialDevice
import com.hack.hbbcompanion.UPnPDiscovery

class MainViewModel(application: Application): AndroidViewModel(application) {
    val deviceList: LiveData<List<DialDevice>> = liveData {
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val data = UPnPDiscovery.discoverDevices(wifiManager)
        getDeviceDescriptions(data)
        emit(data)
    }

    private fun getDeviceDescriptions(devices: List<DialDevice>) {

    }
}
