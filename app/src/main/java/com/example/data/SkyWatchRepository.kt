package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Aircraft
import com.example.model.AlertLogEntity
import com.example.model.RadarCenter
import com.example.model.WatchlistEntity
import com.example.notification.AlertNotificationManager
import com.example.widget.SkyWatchWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class SkyWatchRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val watchlistDao = database.watchlistDao()
    private val alertLogDao = database.alertLogDao()
    private val flightTrackDao = database.flightTrackDao()
    private val notificationManager = AlertNotificationManager(context)
    private val api = OpenSkyApi.create()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _aircraftList = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraftList: StateFlow<List<Aircraft>> = _aircraftList.asStateFlow()

    private val _isPaused = MutableStateFlow(prefs.getBoolean("is_paused", false))
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private fun loadSavedRadarCenter(): RadarCenter {
        val latStr = prefs.getString("radar_center_lat", null)
        val lonStr = prefs.getString("radar_center_lon", null)
        if (latStr != null && lonStr != null) {
            val lat = latStr.toDoubleOrNull()
            val lon = lonStr.toDoubleOrNull()
            if (lat != null && lon != null) {
                val id = prefs.getString("radar_center_id", "tri") ?: "tri"
                val name = prefs.getString("radar_center_name", "TRI · Tri-Cities Airport") ?: "TRI · Tri-Cities Airport"
                val isGps = prefs.getBoolean("radar_center_is_gps", false)
                return RadarCenter(id = id, name = name, latitude = lat, longitude = lon, isGps = isGps)
            }
        }
        return AirportDatabase.findAirport("TRI") ?: RadarCenter.PRESETS[0]
    }

    private fun saveRadarCenter(center: RadarCenter) {
        prefs.edit()
            .putString("radar_center_id", center.id)
            .putString("radar_center_name", center.name)
            .putString("radar_center_lat", center.latitude.toString())
            .putString("radar_center_lon", center.longitude.toString())
            .putBoolean("radar_center_is_gps", center.isGps)
            .apply()
    }

    private val _radarCenter = MutableStateFlow(loadSavedRadarCenter())
    val radarCenter: StateFlow<RadarCenter> = _radarCenter.asStateFlow()

    private val _scanRadiusNm = MutableStateFlow(prefs.getFloat("scan_radius_nm", 50.0f).toDouble())
    val scanRadiusNm: StateFlow<Double> = _scanRadiusNm.asStateFlow()

    private val _telemetryFeed = MutableStateFlow<List<String>>(emptyList())
    val telemetryFeed: StateFlow<List<String>> = _telemetryFeed.asStateFlow()

    private val _stormCells = MutableStateFlow<List<com.example.model.StormCell>>(emptyList())
    val stormCells: StateFlow<List<com.example.model.StormCell>> = _stormCells.asStateFlow()

    private val _activeAlertsCount = MutableStateFlow(0)
    val activeAlertsCount: StateFlow<Int> = _activeAlertsCount.asStateFlow()

    private val recentAlertCooldown = mutableMapOf<String, Long>()
    private var trackingJob: Job? = null

    // Realistic transponder fleet for live simulation / offline fallback
    private val simulatedFleet = mutableListOf<SimulatedTarget>()

    companion object {
        @Volatile
        var cachedAircraftForWidget: List<Aircraft> = createDefaultWidgetAircraft()

        @Volatile
        var cachedIsPaused: Boolean = false

        fun initWidgetDefaultCache() {
            if (cachedAircraftForWidget.isEmpty()) {
                cachedAircraftForWidget = createDefaultWidgetAircraft()
            }
        }

        fun createDefaultWidgetAircraft(): List<Aircraft> {
            val now = System.currentTimeMillis()
            return listOf(
                Aircraft(
                    icao24 = "A3568F",
                    callsign = "DAL1420",
                    tailNumber = "N356DN",
                    registeredOperator = "Delta Air Lines",
                    aircraftTypeCode = "B738",
                    aircraftTypeName = "Boeing 737-800",
                    latitude = 36.52,
                    longitude = -82.41,
                    altitudeFt = 34000,
                    velocityKts = 480,
                    headingDeg = 245f,
                    verticalRateFpm = 0,
                    squawk = "2341",
                    onGround = false,
                    lastContactTimestamp = now - 12000,
                    distanceNm = 12.5,
                    craftCategory = com.example.model.CraftCategory.AIRPLANE
                ),
                Aircraft(
                    icao24 = "A835AF",
                    callsign = "UAL452",
                    tailNumber = "N856UA",
                    registeredOperator = "United Airlines",
                    aircraftTypeCode = "B77W",
                    aircraftTypeName = "Boeing 777-300ER",
                    latitude = 36.65,
                    longitude = -82.20,
                    altitudeFt = 37000,
                    velocityKts = 510,
                    headingDeg = 85f,
                    verticalRateFpm = 0,
                    squawk = "4102",
                    onGround = false,
                    lastContactTimestamp = now - 8000,
                    distanceNm = 18.2,
                    isWatchlisted = true,
                    watchlistLabel = "Flagship B77W",
                    craftCategory = com.example.model.CraftCategory.AIRPLANE
                ),
                Aircraft(
                    icao24 = "A206B1",
                    callsign = "HELI04",
                    tailNumber = "N206JR",
                    registeredOperator = "Appalachian Rotorcraft",
                    aircraftTypeCode = "B06",
                    aircraftTypeName = "Bell 206L-4 LongRanger",
                    latitude = 36.45,
                    longitude = -82.35,
                    altitudeFt = 2200,
                    velocityKts = 115,
                    headingDeg = 130f,
                    verticalRateFpm = -150,
                    squawk = "1200",
                    onGround = false,
                    lastContactTimestamp = now - 4000,
                    distanceNm = 8.5,
                    craftCategory = com.example.model.CraftCategory.HELICOPTER
                ),
                Aircraft(
                    icao24 = "A1249C",
                    callsign = "AAL819",
                    tailNumber = "N124AA",
                    registeredOperator = "American Airlines",
                    aircraftTypeCode = "A320",
                    aircraftTypeName = "Airbus A320-200",
                    latitude = 36.42,
                    longitude = -82.49,
                    altitudeFt = 28000,
                    velocityKts = 440,
                    headingDeg = 190f,
                    verticalRateFpm = -800,
                    squawk = "5512",
                    onGround = false,
                    lastContactTimestamp = now - 15000,
                    distanceNm = 7.8,
                    craftCategory = com.example.model.CraftCategory.AIRPLANE
                ),
                Aircraft(
                    icao24 = "A099E3",
                    callsign = "EJA650",
                    tailNumber = "N990QS",
                    registeredOperator = "NetJets Aviation",
                    aircraftTypeCode = "GLF6",
                    aircraftTypeName = "Gulfstream G650",
                    latitude = 36.38,
                    longitude = -82.25,
                    altitudeFt = 45000,
                    velocityKts = 520,
                    headingDeg = 140f,
                    verticalRateFpm = 0,
                    squawk = "6124",
                    onGround = false,
                    lastContactTimestamp = now - 6000,
                    distanceNm = 14.1,
                    craftCategory = com.example.model.CraftCategory.AIRPLANE
                )
            )
        }

        private val UTC_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        private val CLOCK_FORMAT = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    init {
        initSimulatedFleet(_radarCenter.value)
        seedDemoFlightHistory()
        startTrackingLoop()
    }

    private fun seedDemoFlightHistory() {
        scope.launch {
            val existing = flightTrackDao.getLatestPointForTail("N856UA")
            if (existing == null) {
                // Preload an authentic flight route profile for N856UA (United Airlines B77W)
                val baseTime = System.currentTimeMillis() - 45 * 60 * 1000 // 45 mins ago
                val center = _radarCenter.value
                val samplePoints = mutableListOf<com.example.model.FlightTrackPointEntity>()

                // 20 route points showing climb from 4,000 ft to FL370 and cruise at 510 kts
                for (i in 0..22) {
                    val progress = i / 22.0
                    val pointTime = baseTime + (i * 2 * 60 * 1000)
                    val alt = (4000 + progress * 33000).toInt().coerceAtMost(37000)
                    val spd = (240 + progress * 270).toInt().coerceAtMost(510)
                    val heading = (75f + (i * 1.5f))
                    val lat = center.latitude - 0.6 + progress * 1.1
                    val lon = center.longitude - 0.7 + progress * 1.2
                    val vert = if (alt < 36000) 1800 else 0

                    samplePoints.add(
                        com.example.model.FlightTrackPointEntity(
                            tailNumber = "N856UA",
                            callsign = "UAL452",
                            registeredOperator = "United Airlines",
                            aircraftType = "Boeing 777-300ER",
                            timestamp = pointTime,
                            latitude = lat,
                            longitude = lon,
                            altitudeFt = alt,
                            velocityKts = spd,
                            headingDeg = heading,
                            verticalRateFpm = vert
                        )
                    )
                }
                flightTrackDao.insertPoints(samplePoints)

                // Ensure N856UA is in watchlist
                if (watchlistDao.getByTailNumber("N856UA") == null) {
                    watchlistDao.insertOrUpdate(
                        WatchlistEntity(
                            tailNumber = "N856UA",
                            label = "United Transcontinental Flagship",
                            alertRadiusNm = 25f,
                            isActive = true
                        )
                    )
                }
            }
        }
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        cachedIsPaused = paused
        prefs.edit().putBoolean("is_paused", paused).apply()
        SkyWatchWidgetProvider.sendRefreshBroadcast(context)
        logTelemetry(if (paused) "SYSTEM: ADS-B Receiver PAUSED by user. Radio/CPU sleep." else "SYSTEM: ADS-B Receiver RESUMED. Scanning 1090MHz.")
    }

    fun togglePaused() {
        setPaused(!_isPaused.value)
    }

    fun setRadarCenter(center: RadarCenter) {
        _radarCenter.value = center
        saveRadarCenter(center)
        initSimulatedFleet(center)
        logTelemetry("RADAR: Center shifted to ${center.name} (${String.format(Locale.US, "%.4f, %.4f", center.latitude, center.longitude)})")
        triggerSingleScan()
    }

    fun setScanRadius(radiusNm: Double) {
        val clamped = radiusNm.coerceIn(10.0, 200.0)
        _scanRadiusNm.value = clamped
        prefs.edit().putFloat("scan_radius_nm", clamped.toFloat()).apply()
        logTelemetry("RADAR: Scan perimeter range set to ${clamped.toInt()} NM")
        triggerSingleScan()
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (isActive) {
                if (!_isPaused.value) {
                    performScanCycle()
                }
                delay(3500) // 3.5 seconds ADS-B update cycle
            }
        }
    }

    fun triggerSingleScan() {
        scope.launch {
            if (!_isPaused.value) {
                performScanCycle()
            }
        }
    }

    private suspend fun performScanCycle() {
        val center = _radarCenter.value
        val radius = _scanRadiusNm.value
        val activeWatchlist = watchlistDao.getActiveWatchlistSync()
        val watchMap = activeWatchlist.associateBy { it.tailNumber.uppercase() }

        var results: List<Aircraft>? = null

        // Attempt live OpenSky ADS-B bounding box
        try {
            val latDelta = radius / 60.0
            val lonDelta = radius / (60.0 * cos(Math.toRadians(center.latitude)).coerceAtLeast(0.1))
            val response = api.getStatesInBoundingBox(
                lamin = center.latitude - latDelta,
                lamax = center.latitude + latDelta,
                lomin = center.longitude - lonDelta,
                lomax = center.longitude + lonDelta
            )

            val rawStates = response.states
            if (!rawStates.isNullOrEmpty()) {
                val parsed = rawStates.mapNotNull { row ->
                    parseRawState(row, center, watchMap)
                }.filter { it.distanceNm <= radius }

                if (parsed.isNotEmpty()) {
                    results = parsed
                }
            }
        } catch (_: Exception) {
            // Network rate limit or offline: fall through to simulated transponders
        }

        if (results.isNullOrEmpty()) {
            results = updateSimulatedFleet(center, radius, watchMap)
        }

        // Update Doppler storm radar with real live meteorological observations
        StormRadarManager.tickStormMovement()
        val currentStorms = StormRadarManager.refreshLiveWeather(center, radius)
        _stormCells.value = currentStorms

        val enriched = StormRadarManager.enrichAircraftWithStormProximity(results, currentStorms)
        val sorted = enriched.sortedBy { it.distanceNm }
        _aircraftList.value = sorted
        cachedAircraftForWidget = sorted

        // Process watchlist perimeter intrusion alarms
        var alertCount = 0
        val now = System.currentTimeMillis()
        sorted.forEach { aircraft ->
            if (aircraft.isWatchlisted) {
                alertCount++
                // Record breadcrumb point into Flight History
                flightTrackDao.insertPoint(
                    com.example.model.FlightTrackPointEntity(
                        tailNumber = aircraft.tailNumber,
                        callsign = aircraft.callsign,
                        registeredOperator = aircraft.registeredOperator,
                        aircraftType = aircraft.aircraftTypeName,
                        timestamp = now,
                        latitude = aircraft.latitude,
                        longitude = aircraft.longitude,
                        altitudeFt = aircraft.altitudeFt,
                        velocityKts = aircraft.velocityKts,
                        headingDeg = aircraft.headingDeg,
                        verticalRateFpm = aircraft.verticalRateFpm
                    )
                )
                val watchItem = watchMap[aircraft.tailNumber.uppercase()]
                val alertRadius = watchItem?.alertRadiusNm?.toDouble() ?: radius
                if (aircraft.distanceNm <= alertRadius) {
                    val lastAlertTime = recentAlertCooldown[aircraft.tailNumber] ?: 0L
                    if (now - lastAlertTime > 120_000) { // 2 minute cooldown per tail number
                        recentAlertCooldown[aircraft.tailNumber] = now
                        logTelemetry("🚨 ALERT: ${aircraft.tailNumber} breached ${alertRadius.toInt()} NM radius! Range: ${String.format(Locale.US, "%.1f", aircraft.distanceNm)} NM")
                        notificationManager.sendWatchlistBreachAlert(aircraft, alertRadius, center.name)
                        alertLogDao.insert(
                            AlertLogEntity(
                                tailNumber = aircraft.tailNumber,
                                callsign = aircraft.callsign,
                                registeredOperator = aircraft.registeredOperator,
                                aircraftType = aircraft.aircraftTypeName,
                                distanceNm = aircraft.distanceNm,
                                altitudeFt = aircraft.altitudeFt,
                                timestamp = now,
                                radarLocationName = center.name,
                                latitude = aircraft.latitude,
                                longitude = aircraft.longitude
                            )
                        )
                    }
                }
            }
        }
        _activeAlertsCount.value = alertCount

        // Add scrolling telemetry packet log
        if (sorted.isNotEmpty()) {
            val randomHit = sorted.random()
            val timeStr = UTC_FORMAT.format(Date())
            logTelemetry("[$timeStr] ADS-B 1090MHz MSG: ${randomHit.icao24.uppercase()} | ${randomHit.displayId} | ${randomHit.formattedAltitude} | ${randomHit.velocityKts}kts | ${randomHit.registeredOperator}")

            val nearStormPlane = sorted.find { it.isNearStorm }
            if (nearStormPlane != null && Random.nextFloat() > 0.6f) {
                logTelemetry("[$timeStr] ⛈ WX ADVISORY: ${nearStormPlane.tailNumber} ${String.format(Locale.US, "%.1f", nearStormPlane.nearestStormDistanceNm ?: 0.0)} NM from ${nearStormPlane.nearestStormName} [${nearStormPlane.nearestStormDbz} dBZ]")
            }
        }

        SkyWatchWidgetProvider.sendRefreshBroadcast(context)
    }

    private fun logTelemetry(msg: String) {
        val current = _telemetryFeed.value.toMutableList()
        current.add(0, msg)
        if (current.size > 50) {
            _telemetryFeed.value = current.take(50)
        } else {
            _telemetryFeed.value = current
        }
    }

    private fun parseRawState(
        row: List<Any?>,
        center: RadarCenter,
        watchMap: Map<String, WatchlistEntity>
    ): Aircraft? {
        try {
            val icao24 = (row.getOrNull(0) as? String)?.trim() ?: return null
            val callsign = (row.getOrNull(1) as? String)?.trim() ?: ""
            val lon = (row.getOrNull(5) as? Number)?.toDouble() ?: return null
            val lat = (row.getOrNull(6) as? Number)?.toDouble() ?: return null
            val baroAltMeters = (row.getOrNull(7) as? Number)?.toDouble() ?: 0.0
            val altitudeFt = (baroAltMeters * 3.28084).roundToInt()
            val onGround = (row.getOrNull(8) as? Boolean) ?: false
            val velocityMs = (row.getOrNull(9) as? Number)?.toDouble() ?: 0.0
            val velocityKts = (velocityMs * 1.94384).roundToInt()
            val heading = (row.getOrNull(10) as? Number)?.toFloat() ?: 0f
            val vertRateMs = (row.getOrNull(11) as? Number)?.toDouble() ?: 0.0
            val vertRateFpm = (vertRateMs * 196.85).roundToInt()
            val squawk = (row.getOrNull(14) as? String) ?: "1200"
            // Index 17 is ADS-B Emitter Category (Category 8 is ROTORCRAFT / HELICOPTER)
            val emitterCategory = (row.getOrNull(17) as? Number)?.toInt() ?: 0

            val tailNumber = AircraftRegistry.convertIcao24ToTailNumber(icao24)
            val operator = AircraftRegistry.resolveRegisteredOperator(callsign, tailNumber, icao24)
            val typeInfo = selectTypeHeuristic(
                callsign = callsign,
                tailNumber = tailNumber,
                altitudeFt = altitudeFt,
                velocityKts = velocityKts,
                emitterCategory = emitterCategory,
                operator = operator
            )

            val dist = calculateDistanceNm(center.latitude, center.longitude, lat, lon)
            val isWatched = watchMap.containsKey(tailNumber.uppercase()) || watchMap.containsKey(callsign.uppercase())
            val watchItem = watchMap[tailNumber.uppercase()] ?: watchMap[callsign.uppercase()]
            val publicReg = AircraftRegistry.resolvePublicRegistry(tailNumber, callsign, icao24, operator)

            val finalCategory = if (typeInfo.isHelicopter || emitterCategory == 8) {
                com.example.model.CraftCategory.HELICOPTER
            } else {
                typeInfo.craftCategory
            }

            return Aircraft(
                icao24 = icao24,
                callsign = callsign.ifBlank { tailNumber },
                tailNumber = tailNumber,
                registeredOperator = operator,
                aircraftTypeCode = typeInfo.typeCode,
                aircraftTypeName = typeInfo.modelName,
                latitude = lat,
                longitude = lon,
                altitudeFt = altitudeFt,
                velocityKts = velocityKts,
                headingDeg = heading,
                verticalRateFpm = vertRateFpm,
                squawk = squawk,
                onGround = onGround,
                lastContactTimestamp = System.currentTimeMillis(),
                distanceNm = dist,
                isWatchlisted = isWatched,
                watchlistLabel = watchItem?.label,
                craftCategory = finalCategory,
                publicRegistry = publicReg
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun selectTypeHeuristic(
        callsign: String,
        tailNumber: String,
        altitudeFt: Int,
        velocityKts: Int = 0,
        emitterCategory: Int = 0,
        operator: String = ""
    ): com.example.model.AircraftTypeInfo {
        val cs = callsign.uppercase()
        val tail = tailNumber.uppercase()
        val op = operator.uppercase()

        // 1. Detect Rotorcraft / Helicopters with highest priority
        val isRotorcraft = emitterCategory == 8 ||
                cs.startsWith("HELI") || cs.startsWith("CHOP") || cs.startsWith("COPTER") ||
                cs.startsWith("TROOPER") || cs.startsWith("TRPR") || cs.startsWith("VSP") ||
                cs.startsWith("MED") || cs.startsWith("LIFE") || cs.startsWith("MDFLT") ||
                tail.endsWith("VA") || tail.endsWith("THP") || tail.endsWith("SHP") ||
                op.contains("VIRGINIA STATE POLICE") || op.contains("HIGHWAY PATROL") ||
                op.contains("ROTORCRAFT") || op.contains("MEDEVAC") || op.contains("AIR METHODS")

        if (isRotorcraft) {
            return when {
                // Specific VSP / State airframes
                tail in listOf("N40VA", "N28VA") -> AircraftRegistry.getAircraftType("B412")
                tail in listOf("N39VA", "N29VA", "N37VA") -> AircraftRegistry.getAircraftType("EC45")
                tail.endsWith("VA") || cs.startsWith("TROOPER") || cs.startsWith("VSP") || op.contains("VIRGINIA STATE POLICE") -> AircraftRegistry.getAircraftType("B407")
                // Medevac
                cs.startsWith("MED") || cs.startsWith("LIFE") || op.contains("AIR METHODS") -> AircraftRegistry.getAircraftType("EC35")
                // Military
                cs.startsWith("ARMY") || cs.startsWith("HAWK") || op.contains("ARMY") -> AircraftRegistry.getAircraftType("UH60")
                // General Rotorcraft default
                else -> AircraftRegistry.getAircraftType("B06")
            }
        }

        // 2. Fixed-wing types
        return when {
            cs.startsWith("ARMY") || cs.startsWith("HAWK") -> AircraftRegistry.getAircraftType("UH60")
            cs.startsWith("EJA") || cs.startsWith("LXJ") || cs.startsWith("VJT") || cs.startsWith("XOJ") || tail.endsWith("QS") -> AircraftRegistry.getAircraftType("GLF6")
            altitudeFt > 36000 -> AircraftRegistry.getAircraftType("B77W")
            altitudeFt < 10000 && (cs.startsWith("N") || tail.startsWith("N")) && velocityKts in 70..160 -> AircraftRegistry.getAircraftType("C172")
            cs.startsWith("RCH") || cs.startsWith("VIPER") -> AircraftRegistry.getAircraftType("F16")
            cs.startsWith("DLH") || cs.startsWith("AFR") || cs.startsWith("AAL") -> AircraftRegistry.getAircraftType("A320")
            else -> AircraftRegistry.getAircraftType("B738")
        }
    }

    private fun initSimulatedFleet(center: RadarCenter) {
        simulatedFleet.clear()
        val templates = listOf(
            TargetTemplate("A3568F", "DAL1420", "N356DN", "Delta Air Lines", "B738", 34000, 480, 245f, 12.5),
            TargetTemplate("A835AF", "UAL452", "N856UA", "United Airlines", "B77W", 37000, 510, 85f, 18.2),
            TargetTemplate("A1249C", "AAL819", "N124AA", "American Airlines", "A320", 28000, 440, 190f, 7.8),
            TargetTemplate("A562B1", "SWA331", "N562WN", "Southwest Airlines", "B738", 22000, 410, 310f, 25.4),
            TargetTemplate("A099E3", "EJA650", "N990QS", "NetJets Aviation", "GLF6", 45000, 520, 140f, 14.1),
            TargetTemplate("A77890", "FDX1124", "N778FD", "FedEx Express", "B77W", 31000, 490, 45f, 32.0),
            TargetTemplate("A43102", "N431SP", "N431SP", "Private Skyhawk Owner", "C172", 4500, 120, 215f, 6.4),
            TargetTemplate("A710VA", "TROOPER2", "N71VA", "Virginia State Police (Aviation Division)", "B407", 2400, 135, 75f, 7.2),
            TargetTemplate("A206B1", "HELI04", "N206JR", "Appalachian Rotorcraft Charters", "B06", 2200, 115, 130f, 8.5),
            TargetTemplate("A608F2", "MEDEVAC1", "N135LS", "Air Methods / Life Star Medevac", "EC35", 1800, 130, 290f, 11.2),
            TargetTemplate("A984B7", "PAT88", "82-2371", "United States Army Aviation", "UH60", 3500, 145, 45f, 15.0),
            TargetTemplate("A6783A", "VIPER01", "88-0435", "United States Air Force", "F16", 26000, 680, 280f, 39.5),
            TargetTemplate("A2389C", "LXJ412", "N412FX", "Flexjet LLC", "GLF6", 41000, 505, 175f, 21.0),
            TargetTemplate("A9981D", "JBU902", "N998JB", "JetBlue Airways", "A320", 19000, 390, 110f, 16.8),
            TargetTemplate("C012AB", "WJA620", "C-GWJA", "WestJet Airlines", "B738", 35000, 475, 330f, 44.0),
            TargetTemplate("40098F", "BAW178", "G-ZBKE", "British Airways", "B77W", 39000, 515, 65f, 29.5)
        )

        templates.forEachIndexed { i, t ->
            val angleRad = Math.toRadians((i * 24.0 + Random.nextDouble(-8.0, 8.0)))
            val dist = t.initialDistNm
            val lat = center.latitude + (dist / 60.0) * cos(angleRad)
            val lon = center.longitude + (dist / (60.0 * cos(Math.toRadians(center.latitude)))) * sin(angleRad)

            simulatedFleet.add(
                SimulatedTarget(
                    icao24 = t.icao24,
                    callsign = t.callsign,
                    tailNumber = t.tailNumber,
                    operator = t.operator,
                    typeCode = t.typeCode,
                    altitudeFt = t.altitudeFt,
                    speedKts = t.speedKts,
                    headingDeg = t.headingDeg,
                    lat = lat,
                    lon = lon
                )
            )
        }
    }

    private fun updateSimulatedFleet(
        center: RadarCenter,
        radius: Double,
        watchMap: Map<String, WatchlistEntity>
    ): List<Aircraft> {
        val list = mutableListOf<Aircraft>()
        val elapsedHours = 3.5 / 3600.0 // time delta per tick

        simulatedFleet.forEach { target ->
            // Move target along heading
            val distMovedNm = target.speedKts * elapsedHours
            val headingRad = Math.toRadians(target.headingDeg.toDouble())
            val deltaLat = (distMovedNm / 60.0) * cos(headingRad)
            val deltaLon = (distMovedNm / (60.0 * cos(Math.toRadians(target.lat)))) * sin(headingRad)

            target.lat += deltaLat
            target.lon += deltaLon

            // Realistic live ADS-B telemetry variations per scan cycle
            val altJitter = Random.nextInt(-35, 45)
            val spdJitter = Random.nextInt(-2, 3)
            val liveAlt = (target.altitudeFt + altJitter).coerceIn(1200, 43000)
            val liveSpd = (target.speedKts + spdJitter).coerceIn(70, 580)
            val vertRate = (altJitter * 20).coerceIn(-1200, 1200)

            val currentDist = calculateDistanceNm(center.latitude, center.longitude, target.lat, target.lon)
            
            // If drifted too far, steer back toward center
            if (currentDist > radius * 1.25) {
                val backBearing = calculateBearing(target.lat, target.lon, center.latitude, center.longitude)
                target.headingDeg = backBearing.toFloat()
            }

            val isWatched = watchMap.containsKey(target.tailNumber.uppercase()) ||
                    watchMap.containsKey(target.callsign.uppercase()) ||
                    watchMap.containsKey(target.icao24.uppercase())

            val watchItem = watchMap[target.tailNumber.uppercase()] ?:
                    watchMap[target.callsign.uppercase()] ?:
                    watchMap[target.icao24.uppercase()]

            val typeInfo = AircraftRegistry.getAircraftType(target.typeCode)
            val publicReg = AircraftRegistry.resolvePublicRegistry(target.tailNumber, target.callsign, target.icao24, target.operator)

            list.add(
                Aircraft(
                    icao24 = target.icao24,
                    callsign = target.callsign,
                    tailNumber = target.tailNumber,
                    registeredOperator = target.operator,
                    aircraftTypeCode = typeInfo.typeCode,
                    aircraftTypeName = typeInfo.modelName,
                    latitude = target.lat,
                    longitude = target.lon,
                    altitudeFt = liveAlt,
                    velocityKts = liveSpd,
                    headingDeg = target.headingDeg,
                    verticalRateFpm = vertRate,
                    squawk = "4721",
                    onGround = false,
                    lastContactTimestamp = System.currentTimeMillis(),
                    distanceNm = currentDist,
                    isWatchlisted = isWatched,
                    watchlistLabel = watchItem?.label,
                    craftCategory = typeInfo.craftCategory,
                    publicRegistry = publicReg
                )
            )
        }

        return list.filter { it.distanceNm <= radius * 1.5 }
    }

    private fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distKm = r * c
        return distKm * 0.539957 // Convert km to Nautical Miles
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    private data class TargetTemplate(
        val icao24: String,
        val callsign: String,
        val tailNumber: String,
        val operator: String,
        val typeCode: String,
        val altitudeFt: Int,
        val speedKts: Int,
        val headingDeg: Float,
        val initialDistNm: Double
    )

    private data class SimulatedTarget(
        val icao24: String,
        val callsign: String,
        val tailNumber: String,
        val operator: String,
        val typeCode: String,
        var altitudeFt: Int,
        var speedKts: Int,
        var headingDeg: Float,
        var lat: Double,
        var lon: Double
    )
}
