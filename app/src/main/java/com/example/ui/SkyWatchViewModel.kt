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

    private val _selectedTailForHistory = MutableStateFlow<String?>("N856UA")
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

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _connectedCastDevice = MutableStateFlow<com.example.model.CastDevice?>(null)
    val connectedCastDevice: StateFlow<com.example.model.CastDevice?> = _connectedCastDevice.asStateFlow()

    private val _availableCastDevices = MutableStateFlow(com.example.model.CastDevice.DEFAULT_DEVICES)
    val availableCastDevices: StateFlow<List<com.example.model.CastDevice>> = _availableCastDevices.asStateFlow()

    private val _selectedAircraft = MutableStateFlow<Aircraft?>(null)
    val selectedAircraft: StateFlow<Aircraft?> = _selectedAircraft.asStateFlow()

    private val _selectedTypeForInfo = MutableStateFlow<AircraftTypeInfo?>(null)
    val selectedTypeForInfo: StateFlow<AircraftTypeInfo?> = _selectedTypeForInfo.asStateFlow()

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
        _connectedCastDevice.value = device.copy(isConnected = true)
        _isCasting.value = true
        _availableCastDevices.value = _availableCastDevices.value.map {
            it.copy(isConnected = it.id == device.id)
        }
    }

    fun disconnectCast() {
        _connectedCastDevice.value = null
        _isCasting.value = false
        _availableCastDevices.value = _availableCastDevices.value.map {
            it.copy(isConnected = false)
        }
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

    fun selectAircraft(aircraft: Aircraft) {
        _selectedAircraft.value = aircraft
    }

    fun showTypeDetails(typeInfo: AircraftTypeInfo) {
        _selectedTypeForInfo.value = typeInfo
    }

    fun dismissTypeDetails() {
        _selectedTypeForInfo.value = null
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
            val sampleAircraft = aircraftList.value.firstOrNull() ?: Aircraft(
                icao24 = "A835AF",
                callsign = "UAL452",
                tailNumber = "N856UA",
                registeredOperator = "United Airlines",
                aircraftTypeCode = "B77W",
                aircraftTypeName = "Boeing 777-300ER",
                latitude = radarCenter.value.latitude + 0.05,
                longitude = radarCenter.value.longitude + 0.05,
                altitudeFt = 24000,
                velocityKts = 480,
                headingDeg = 245f,
                verticalRateFpm = -500,
                squawk = "4721",
                onGround = false,
                lastContactTimestamp = System.currentTimeMillis(),
                distanceNm = 8.4,
                isWatchlisted = true
            )

            val alert = AlertLogEntity(
                tailNumber = sampleAircraft.tailNumber,
                callsign = sampleAircraft.callsign,
                registeredOperator = sampleAircraft.registeredOperator,
                aircraftType = sampleAircraft.aircraftTypeName,
                distanceNm = sampleAircraft.distanceNm,
                altitudeFt = sampleAircraft.altitudeFt,
                timestamp = System.currentTimeMillis(),
                radarLocationName = radarCenter.value.name,
                latitude = sampleAircraft.latitude,
                longitude = sampleAircraft.longitude
            )
            alertLogDao.insert(alert)

            com.example.notification.AlertNotificationManager(getApplication())
                .sendWatchlistBreachAlert(sampleAircraft, 25.0, radarCenter.value.name)
        }
    }
}
