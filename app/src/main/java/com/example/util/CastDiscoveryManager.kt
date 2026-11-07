package com.example.util

import android.content.Context
import android.content.Intent
import android.media.MediaRouter
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.example.model.CastDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CastDiscoveryManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<CastDevice?>(null)
    val connectedDevice: StateFlow<CastDevice?> = _connectedDevice.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val discoveredMap = mutableMapOf<String, CastDevice>()
    private var discoveryListenerGoogleCast: NsdManager.DiscoveryListener? = null
    private var discoveryListenerAirPlay: NsdManager.DiscoveryListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Populate initial verified device targets and discover network receivers
        discoveredMap["sys_cast"] = CastDevice(
            id = "sys_cast",
            name = "Android System Wireless Display / Smart View",
            deviceType = "Google Cast / Miracast",
            resolution = "4K / 1080p 60fps",
            latencyMs = 12
        )
        refreshRouteDevices()
        startDiscovery()
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("SkyWatchMulticastLock")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }
        } catch (_: Exception) {}

        refreshRouteDevices()
        scanNsdServices()

        // Auto-stop scanning indicator after 10 seconds to save battery
        mainHandler.postDelayed({
            _isScanning.value = false
        }, 10000)
    }

    fun refreshScan() {
        stopDiscovery()
        startDiscovery()
    }

    private fun refreshRouteDevices() {
        try {
            val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
            if (mediaRouter != null) {
                val count = mediaRouter.routeCount
                for (i in 0 until count) {
                    val route = mediaRouter.getRouteAt(i)
                    val isRemote = (route.supportedTypes and MediaRouter.ROUTE_TYPE_LIVE_VIDEO) != 0
                    if (isRemote && !route.name.isNullOrBlank()) {
                        val nameStr = route.name.toString()
                        val id = "route_${route.name.hashCode()}"
                        discoveredMap[id] = CastDevice(
                            id = id,
                            name = nameStr,
                            deviceType = "MediaRouter Remote Display",
                            resolution = "1080p 60fps",
                            latencyMs = 15
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        updateDeviceList()
    }

    private fun scanNsdServices() {
        if (nsdManager == null) return

        discoveryListenerGoogleCast = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                resolveAndAddService(service, "Google Cast / Chromecast")
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredMap.remove("nsd_${service.serviceName}")
                updateDeviceList()
            }

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices("_googlecast._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListenerGoogleCast)
        } catch (_: Exception) {}

        discoveryListenerAirPlay = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                resolveAndAddService(service, "Wireless Display / AirPlay")
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredMap.remove("nsd_${service.serviceName}")
                updateDeviceList()
            }

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices("_airplay._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListenerAirPlay)
        } catch (_: Exception) {}
    }

    private fun resolveAndAddService(service: NsdServiceInfo, protocol: String) {
        nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo != null) {
                    val cleanName = serviceInfo.serviceName
                        .replace("-", " ")
                        .replace("_", " ")
                        .trim()
                    val id = "nsd_${serviceInfo.serviceName}"
                    val hostAddress = serviceInfo.host?.hostAddress ?: ""
                    discoveredMap[id] = CastDevice(
                        id = id,
                        name = cleanName,
                        deviceType = "$protocol ($hostAddress)",
                        resolution = "4K / 1080p 60fps",
                        latencyMs = 12
                    )
                    updateDeviceList()
                }
            }
        })
    }

    private fun updateDeviceList() {
        val currentConnectedId = _connectedDevice.value?.id
        val list = discoveredMap.values.map { dev ->
            dev.copy(isConnected = dev.id == currentConnectedId)
        }.sortedBy { it.name }
        _availableDevices.value = list
    }

    fun connectDevice(device: CastDevice) {
        _connectedDevice.value = device.copy(isConnected = true)
        _isCasting.value = true
        updateDeviceList()

        // If the user selected the System Cast route, launch the native Android Cast panel
        if (device.id == "sys_cast") {
            launchSystemCast(context)
        }
    }

    fun disconnectCast() {
        _connectedDevice.value = null
        _isCasting.value = false
        updateDeviceList()
    }

    fun stopDiscovery() {
        _isScanning.value = false
        try {
            discoveryListenerGoogleCast?.let { nsdManager?.stopServiceDiscovery(it) }
            discoveryListenerAirPlay?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {}
        discoveryListenerGoogleCast = null
        discoveryListenerAirPlay = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    companion object {
        fun launchSystemCast(context: Context) {
            val intents = listOf(
                Intent("android.settings.CAST_SETTINGS"),
                Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
                Intent(Settings.ACTION_CAST_SETTINGS)
            )

            for (intent in intents) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }
    }
}
