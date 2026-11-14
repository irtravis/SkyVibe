package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AircraftRegistry
import com.example.data.AppDatabase
import com.example.data.SkyWatchRepository
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.AlertLogEntity
import com.example.model.RadarCenter
import com.example.model.WatchlistEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SkyWatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SkyWatchRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val watchlistDao = database.watchlistDao()
    private val alertLogDao = database.alertLogDao()
    private val flightTrackDao = database.flightTrackDao()

    val aircraftList: StateFlow<List<Aircraft>> = repository.aircraftList
    val isPaused: StateFlow<Boolean> = repository.isPaused
    val feedStatus: StateFlow<String> = repository.feedStatus
    val radarCenter: StateFlow<RadarCenter> = repository.radarCenter
    val scanRadiusNm: StateFlow<Double> = repository.scanRadiusNm
    val telemetryFeed: StateFlow<List<String>> = repository.telemetryFeed
    val activeAlertsCount: StateFlow<Int> = repository.activeAlertsCount
    val stormCells: StateFlow<List<com.example.model.StormCell>> = repository.stormCells

    private val _isStormRadarEnabled = MutableStateFlow(true)
    val isStormRadarEnabled: StateFlow<Boolean> = _isStormRadarEnabled.asStateFlow()

    fun toggleStormRadar() {
        _isStormRadarEnabled.value = !_isStormRadarEnabled.value
    }

    fun setStormRadarEnabled(enabled: Boolean) {
        _isStormRadarEnabled.value = enabled
    }

    val watchlist: StateFlow<List<WatchlistEntity>> = watchlistDao.getAllWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertLogs: StateFlow<List<AlertLogEntity>> = alertLogDao.getRecentAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flightTracks: StateFlow<List<com.example.model.FlightTrackPointEntity>> = flightTrackDao.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackedTails: StateFlow<List<String>> = flightTrackDao.getDistinctTrackedTails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTailForHistory = MutableStateFlow<String?>(null)
    val selectedTailForHistory: StateFlow<String?> = _selectedTailForHistory.asStateFlow()

    fun selectTailForHistory(tail: String?) {
        _selectedTailForHistory.value = tail
    }

    fun clearFlightTracks() {
        viewModelScope.launch {
            flightTrackDao.clearAllTracks()
        }
    }

    private val _currentViewMode = MutableStateFlow("MAP") // "MAP", "LIST", "WATCHLIST", "HISTORY"
    val currentViewMode: StateFlow<String> = _currentViewMode.asStateFlow()

    private val _mapLayerType = MutableStateFlow(com.example.model.MapLayerType.RADAR)
    val mapLayerType: StateFlow<com.example.model.MapLayerType> = _mapLayerType.asStateFlow()

    private val castDiscoveryManager = com.example.util.CastDiscoveryManager(application)

    val isCasting: StateFlow<Boolean> = castDiscoveryManager.isCasting
    val connectedCastDevice: StateFlow<com.example.model.CastDevice?> = castDiscoveryManager.connectedDevice
    val availableCastDevices: StateFlow<List<com.example.model.CastDevice>> = castDiscoveryManager.availableDevices
    val isScanningCast: StateFlow<Boolean> = castDiscoveryManager.isScanning

    private val _selectedIcaoOrTail = MutableStateFlow<String?>(null)

    val selectedAircraft: StateFlow<Aircraft?> = combine(
        aircraftList,
        _selectedIcaoOrTail
    ) { list, key ->
        if (key == null) null
        else list.find {
            it.icao24.equals(key, ignoreCase = true) ||
            it.tailNumber.equals(key, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTypeForInfo = MutableStateFlow<AircraftTypeInfo?>(null)
    val selectedTypeForInfo: StateFlow<AircraftTypeInfo?> = _selectedTypeForInfo.asStateFlow()

    private val _selectedKeyForInfo = MutableStateFlow<String?>(null)
    private val _fallbackAircraftForInfo = MutableStateFlow<Aircraft?>(null)

    val selectedAircraftForInfo: StateFlow<Aircraft?> = combine(
        aircraftList,
        _selectedKeyForInfo,
        _fallbackAircraftForInfo
    ) { list, key, fallback ->
        if (key == null) null
        else list.find {
            it.icao24.equals(key, ignoreCase = true) ||
            it.tailNumber.equals(key, ignoreCase = true)
        } ?: fallback?.takeIf { System.currentTimeMillis() - it.lastContactTimestamp <= 45_000L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLocatingGps = MutableStateFlow(false)
    val isLocatingGps: StateFlow<Boolean> = _isLocatingGps.asStateFlow()

    private val _locationMessage = MutableStateFlow<String?>(null)
    val locationMessage: StateFlow<String?> = _locationMessage.asStateFlow()

    fun setViewMode(mode: String) {
        _currentViewMode.value = mode
    }

    fun toggleViewMode() {
        _currentViewMode.value = if (_currentViewMode.value == "MAP") "LIST" else "MAP"
    }

    fun setMapLayerType(layer: com.example.model.MapLayerType) {
        _mapLayerType.value = layer
    }

    fun cycleMapLayerType() {
        val layers = com.example.model.MapLayerType.values()
        val nextIndex = (layers.indexOf(_mapLayerType.value) + 1) % layers.size
        _mapLayerType.value = layers[nextIndex]
    }

    fun connectCastDevice(device: com.example.model.CastDevice) {
        castDiscoveryManager.connectDevice(device)
    }

    fun disconnectCast() {
        castDiscoveryManager.disconnectCast()
    }

    fun refreshCastScan() {
        castDiscoveryManager.refreshScan()
    }

    fun launchSystemCast() {
        com.example.util.CastDiscoveryManager.launchSystemCast(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        castDiscoveryManager.stopDiscovery()
    }

    fun togglePaused() {
        repository.togglePaused()
    }

    fun setRadarCenter(center: RadarCenter) {
        repository.setRadarCenter(center)
        _locationMessage.value = null
    }

    fun setScanRadius(radiusNm: Double) {
        repository.setScanRadius(radiusNm)
    }

    fun searchAndSetAirport(query: String): Boolean {
        val found = com.example.data.AirportDatabase.findAirport(query)
        return if (found != null) {
            repository.setRadarCenter(found)
            _locationMessage.value = "Radar locked to ${found.name}"
            true
        } else {
            _locationMessage.value = "Unknown airport code '$query'. Try e.g. TRI, JFK, LAX"
            false
        }
    }

    fun acquireGpsLocation() {
        viewModelScope.launch {
            _isLocatingGps.value = true
            _locationMessage.value = "Acquiring GPS satellite fix..."
            try {
                val loc = com.example.util.LocationHelper.getCurrentLocation(getApplication())
                if (loc != null) {
                    val gpsCenter = RadarCenter(
                        id = "gps",
                        name = "GPS Position (${String.format(java.util.Locale.US, "%.3f, %.3f", loc.latitude, loc.longitude)})",
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        isGps = true
                    )
                    repository.setRadarCenter(gpsCenter)
                    _locationMessage.value = "GPS Location Lock: ${String.format(java.util.Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)}"
                } else {
                    _locationMessage.value = "GPS fix timed out. Ensure location services are enabled."
                }
            } catch (e: Exception) {
                _locationMessage.value = "Location error: ${e.localizedMessage ?: "Permission required"}"
            } finally {
                _isLocatingGps.value = false
            }
        }
    }

    fun selectAircraft(aircraft: Aircraft?) {
        _selectedIcaoOrTail.value = aircraft?.icao24 ?: aircraft?.tailNumber
    }

    fun clearSelectedAircraft() {
        _selectedIcaoOrTail.value = null
    }

    fun showTypeDetails(typeInfo: AircraftTypeInfo, aircraft: Aircraft? = null) {
        _selectedTypeForInfo.value = typeInfo
        _selectedKeyForInfo.value = aircraft?.icao24 ?: aircraft?.tailNumber
        _fallbackAircraftForInfo.value = aircraft
    }

    fun dismissTypeDetails() {
        _selectedTypeForInfo.value = null
        _selectedKeyForInfo.value = null
        _fallbackAircraftForInfo.value = null
    }

    fun toggleWatchlist(tailNumber: String) {
        viewModelScope.launch {
            val existing = watchlistDao.getByTailNumber(tailNumber.uppercase())
            if (existing != null) {
                watchlistDao.delete(existing)
            } else {
                watchlistDao.insertOrUpdate(
                    WatchlistEntity(
                        tailNumber = tailNumber.uppercase(),
                        label = "Monitored Target",
                        alertRadiusNm = 25f,
                        isActive = true
                    )
                )
            }
            repository.triggerSingleScan()
        }
    }

    fun addWatchlistEntry(tailNumber: String, label: String, radiusNm: Float) {
        viewModelScope.launch {
            watchlistDao.insertOrUpdate(
                WatchlistEntity(
                    tailNumber = tailNumber.uppercase().trim(),
                    label = label.trim(),
                    alertRadiusNm = radiusNm,
                    isActive = true
                )
            )
            repository.triggerSingleScan()
        }
    }

    fun toggleWatchlistActive(entity: WatchlistEntity) {
        viewModelScope.launch {
            watchlistDao.update(entity.copy(isActive = !entity.isActive))
            repository.triggerSingleScan()
        }
    }

    fun deleteWatchlistEntry(entity: WatchlistEntity) {
        viewModelScope.launch {
            watchlistDao.delete(entity)
            repository.triggerSingleScan()
        }
    }

    fun clearAlertLogs() {
        viewModelScope.launch {
            alertLogDao.clearAll()
        }
    }

    fun triggerTestAlarm() {
        viewModelScope.launch {
            val liveTarget = aircraftList.value.firstOrNull() ?: return@launch

            val alert = AlertLogEntity(
                tailNumber = liveTarget.tailNumber,
                callsign = liveTarget.callsign,
                registeredOperator = liveTarget.registeredOperator,
                aircraftType = liveTarget.aircraftTypeName,
                distanceNm = liveTarget.distanceNm,
                altitudeFt = liveTarget.altitudeFt,
                timestamp = System.currentTimeMillis(),
                radarLocationName = radarCenter.value.name,
                latitude = liveTarget.latitude,
                longitude = liveTarget.longitude
            )
            alertLogDao.insert(alert)

            com.example.notification.AlertNotificationManager(getApplication())
                .sendWatchlistBreachAlert(liveTarget, scanRadiusNm.value, radarCenter.value.name)
        }
    }
}
