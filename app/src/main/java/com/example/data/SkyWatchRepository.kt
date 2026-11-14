package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Aircraft
import com.example.model.AlertLogEntity
import com.example.model.CraftCategory
import com.example.model.FlightTrackPointEntity
import com.example.model.RadarCenter
import com.example.model.StormCell
import com.example.model.WatchlistEntity
import com.example.notification.AlertNotificationManager
import com.example.widget.SkyWatchWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Clean, hallucination-free SkyWatch live ADS-B repository.
 * Polls live readsb v2 aggregators (adsb.lol, airplanes.live, adsb.fi) and OpenSky fallback.
 * Strictly zero simulated targets, zero guessed routes, and immediate removal of landed/stale craft.
 */
class SkyWatchRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val watchlistDao = database.watchlistDao()
    private val alertLogDao = database.alertLogDao()
    private val flightTrackDao = database.flightTrackDao()
    private val notificationManager = AlertNotificationManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _aircraftList = MutableStateFlow<List<Aircraft>>(emptyList())
    val aircraftList: StateFlow<List<Aircraft>> = _aircraftList.asStateFlow()

    private val _isPaused = MutableStateFlow(prefs.getBoolean("is_paused", false))
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _feedStatus = MutableStateFlow(
        if (prefs.getBoolean("is_paused", false)) "PAUSED · RADIO SLEEP"
        else "SCANNING LIVE 1090MHz..."
    )
    val feedStatus: StateFlow<String> = _feedStatus.asStateFlow()

    private val _radarCenter = MutableStateFlow(loadSavedRadarCenter())
    val radarCenter: StateFlow<RadarCenter> = _radarCenter.asStateFlow()

    private val _scanRadiusNm = MutableStateFlow(prefs.getFloat("scan_radius_nm", 50.0f).toDouble())
    val scanRadiusNm: StateFlow<Double> = _scanRadiusNm.asStateFlow()

    private val _telemetryFeed = MutableStateFlow<List<String>>(emptyList())
    val telemetryFeed: StateFlow<List<String>> = _telemetryFeed.asStateFlow()

    private val _stormCells = MutableStateFlow<List<StormCell>>(emptyList())
    val stormCells: StateFlow<List<StormCell>> = _stormCells.asStateFlow()

    private val _activeAlertsCount = MutableStateFlow(0)
    val activeAlertsCount: StateFlow<Int> = _activeAlertsCount.asStateFlow()

    private val recentAlertCooldown = mutableMapOf<String, Long>()
    private val scanEpoch = AtomicLong(0L)
    private val scanMutex = Mutex()
    private var trackingJob: Job? = null
    private var enrichmentJob: Job? = null

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
        private const val OFFLINE_STALE_TIMEOUT_MS = 30_000L

        @Volatile
        var cachedAircraftForWidget: List<Aircraft> = emptyList()

        @Volatile
        var cachedIsPaused: Boolean = false

        private val UTC_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    init {
        cachedIsPaused = _isPaused.value
        startTrackingLoop()
    }

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

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        cachedIsPaused = paused
        prefs.edit().putBoolean("is_paused", paused).apply()
        if (paused) {
            enrichmentJob?.cancel()
            _feedStatus.value = "PAUSED · RADIO SLEEP"
        } else {
            _feedStatus.value = "RESUMING LIVE 1090MHz..."
            triggerSingleScan()
        }
        SkyWatchWidgetProvider.sendRefreshBroadcast(context)
        logTelemetry(
            if (paused) "SYSTEM: ADS-B Receiver PAUSED by user. Radio/CPU sleep."
            else "SYSTEM: ADS-B Receiver RESUMED. Polling live feeds."
        )
    }

    fun togglePaused() {
        setPaused(!_isPaused.value)
    }

    fun setRadarCenter(center: RadarCenter) {
        scanEpoch.incrementAndGet()
        enrichmentJob?.cancel()
        _radarCenter.value = center
        saveRadarCenter(center)
        _aircraftList.value = emptyList()
        cachedAircraftForWidget = emptyList()
        _activeAlertsCount.value = 0
        if (!_isPaused.value) {
            _feedStatus.value = "SCANNING ${center.id.uppercase()} AIRSPACE..."
        }
        SkyWatchWidgetProvider.sendRefreshBroadcast(context)
        logTelemetry(
            "RADAR: Center shifted to ${center.name} (${
                String.format(Locale.US, "%.4f, %.4f", center.latitude, center.longitude)
            })"
        )
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
                delay(POLL_INTERVAL_MS)
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

    private suspend fun performScanCycle() = scanMutex.withLock {
        val epochAtStart = scanEpoch.get()
        val center = _radarCenter.value
        val radius = _scanRadiusNm.value
        val activeWatchlist = watchlistDao.getActiveWatchlistSync()
        val watchMap = activeWatchlist.associateBy { it.tailNumber.uppercase() }

        val pollResult = AdsBApiService.fetchLiveAirspace(
            centerLat = center.latitude,
            centerLon = center.longitude,
            radiusNm = radius
        )

        // Bail out if user switched airport/center while network request was in flight
        if (scanEpoch.get() != epochAtStart) return@withLock

        StormRadarManager.tickStormMovement()
        val currentStorms = StormRadarManager.refreshLiveWeather(center, radius)
        _stormCells.value = currentStorms

        val now = System.currentTimeMillis()
        val sorted: List<Aircraft>

        if (pollResult.isSuccess) {
            val mapped = pollResult.targets.mapNotNull { target ->
                mapLiveTargetToAircraft(
                    target = target,
                    scanTimestampMs = pollResult.timestampMs,
                    center = center,
                    radius = radius,
                    watchMap = watchMap
                )
            }
            val enriched = StormRadarManager.enrichAircraftWithStormProximity(mapped, currentStorms)
            sorted = enriched.sortedBy { it.distanceNm }
            _feedStatus.value = "LIVE (${pollResult.source}) · ${sorted.size} airborne"
        } else {
            // Transient failure: do NOT fabricate aircraft; prune contacts older than 30s so landed craft disappear
            val retained = _aircraftList.value.filter { ac ->
                !ac.onGround && (now - ac.lastContactTimestamp) <= OFFLINE_STALE_TIMEOUT_MS
            }.filter { ac ->
                calculateDistanceNm(center.latitude, center.longitude, ac.latitude, ac.longitude) <= radius
            }
            sorted = StormRadarManager.enrichAircraftWithStormProximity(retained, currentStorms)
                .sortedBy { it.distanceNm }
            _feedStatus.value = "SIGNAL LOST (${pollResult.errorMessage ?: "offline"}) · ${sorted.size} cached"
            logTelemetry("[${UTC_FORMAT.format(Date(now))}] FEED WARNING: ${pollResult.errorMessage ?: "No ADS-B connectivity"}")
        }

        if (scanEpoch.get() != epochAtStart) return@withLock

        _aircraftList.value = sorted
        cachedAircraftForWidget = sorted

        processWatchlistAndTelemetry(sorted, watchMap, center, radius, now, pollResult)
        SkyWatchWidgetProvider.sendRefreshBroadcast(context)

        // Lazily enrich any airborne targets missing type/model/route without stalling radar updates
        if (pollResult.isSuccess && sorted.isNotEmpty()) {
            scheduleBackgroundEnrichment(epochAtStart, sorted)
        }
    }

    private fun mapLiveTargetToAircraft(
        target: LiveAdsbTarget,
        scanTimestampMs: Long,
        center: RadarCenter,
        radius: Double,
        watchMap: Map<String, WatchlistEntity>
    ): Aircraft? {
        val dist = calculateDistanceNm(center.latitude, center.longitude, target.latitude, target.longitude)
        if (dist > radius) return null

        AircraftDatabaseService.recordFromLiveAdsb(target)

        val hexUpper = target.icao24.trim().uppercase()
        val tailNumber = target.registration.trim().uppercase()
            .ifBlank { AircraftRegistry.convertIcao24ToTailNumber(hexUpper) }

        val cachedMeta = AircraftDatabaseService.getCachedOrFleet(
            tailNumber = tailNumber,
            icao24 = hexUpper,
            operatorHint = target.registeredOwner
        )

        val resolvedTail = tailNumber.ifBlank { cachedMeta?.tailNumber.orEmpty() }.ifBlank { hexUpper }
        val resolvedTypeCode = target.icaoTypeCode.ifBlank { cachedMeta?.icaoTypeCode.orEmpty() }
        val isHeliHint = target.category.equals("A7", ignoreCase = true) ||
                resolvedTypeCode in Aircraft.ROTORCRAFT_ICAO_TYPES ||
                cachedMeta?.isHelicopter == true

        val descHint = target.description.ifBlank { cachedMeta?.model.orEmpty() }
        val typeInfo = AircraftRegistry.getAircraftType(
            code = resolvedTypeCode,
            descriptionHint = descHint,
            isHelicopterHint = isHeliHint
        )

        val resolvedOperator = target.registeredOwner
            .ifBlank { cachedMeta?.operator.orEmpty() }
            .ifBlank { AircraftRegistry.resolveRegisteredOperator(target.callsign, resolvedTail, hexUpper) }

        val resolvedModelName = when {
            cachedMeta?.model?.isNotBlank() == true -> cachedMeta.model
            target.description.isNotBlank() -> target.description
            else -> typeInfo.modelName
        }

        val finalCategory = if (isHeliHint || typeInfo.isHelicopter) {
            CraftCategory.HELICOPTER
        } else {
            typeInfo.craftCategory
        }

        val watchItem = watchMap[resolvedTail.uppercase()]
            ?: watchMap[target.callsign.uppercase()]
            ?: watchMap[hexUpper]
        val isWatched = watchItem != null

        val contactTimestamp = (scanTimestampMs - (target.seenSec * 1000.0).toLong().coerceAtLeast(0L))
            .coerceAtMost(scanTimestampMs)

        val publicReg = AircraftRegistry.resolvePublicRegistry(
            tailNumber = resolvedTail,
            callsign = target.callsign,
            icao24 = hexUpper,
            operator = resolvedOperator
        )

        val cachedRoute = FlightRouteDatabase.getCachedRoute(target.callsign)

        return Aircraft(
            icao24 = target.icao24,
            callsign = target.callsign,
            tailNumber = resolvedTail,
            registeredOperator = resolvedOperator,
            aircraftTypeCode = if (resolvedTypeCode.isNotBlank()) resolvedTypeCode else typeInfo.typeCode,
            aircraftTypeName = resolvedModelName,
            latitude = target.latitude,
            longitude = target.longitude,
            altitudeFt = target.altitudeFt,
            velocityKts = target.velocityKts,
            headingDeg = target.headingDeg,
            verticalRateFpm = target.verticalRateFpm,
            squawk = target.squawk,
            onGround = false,
            lastContactTimestamp = contactTimestamp,
            distanceNm = dist,
            isWatchlisted = isWatched,
            watchlistLabel = watchItem?.label,
            craftCategory = finalCategory,
            emitterCategory = target.category,
            sourceFeed = target.sourceFeed,
            publicRegistry = publicReg,
            flightPlan = cachedRoute
        )
    }

    private fun scheduleBackgroundEnrichment(epoch: Long, candidates: List<Aircraft>) {
        enrichmentJob?.cancel()
        val needsLookup = candidates.filter { ac ->
            val missingType = ac.aircraftTypeCode == "UNK" ||
                    ac.aircraftTypeName.contains("Pending", ignoreCase = true) ||
                    ac.registeredOperator.contains("Unlisted", ignoreCase = true)
            val missingRoute = ac.callsign.length >= 4 && ac.flightPlan == null && FlightRouteDatabase.getCachedRoute(ac.callsign) == null
            missingType || missingRoute
        }.take(8)

        if (needsLookup.isEmpty()) return

        enrichmentJob = scope.launch {
            val enrichedByHex = coroutineScope {
                needsLookup.map { ac ->
                    async {
                        if (scanEpoch.get() != epoch) return@async null
                        val meta = if (ac.aircraftTypeCode == "UNK" || ac.aircraftTypeName.contains("Pending", ignoreCase = true)) {
                            AircraftDatabaseService.lookupAircraft(ac.tailNumber, ac.icao24, ac.registeredOperator)
                        } else {
                            null
                        }
                        val route = if (ac.callsign.length >= 4 && ac.flightPlan == null) {
                            AircraftDatabaseService.lookupLiveRoute(ac.callsign)
                        } else {
                            null
                        }
                        if (meta != null || route != null) Triple(ac.icao24.lowercase(), meta, route) else null
                    }
                }.awaitAll().filterNotNull().associateBy { it.first }
            }

            if (enrichedByHex.isEmpty() || scanEpoch.get() != epoch) return@launch

            val current = _aircraftList.value
            if (current.isEmpty()) return@launch

            val updated = current.map { ac ->
                val patch = enrichedByHex[ac.icao24.lowercase()] ?: return@map ac
                val (_, meta, route) = patch
                var next = ac
                if (meta != null) {
                    val mergedType = meta.icaoTypeCode.ifBlank { ac.aircraftTypeCode }
                    val mergedTail = meta.tailNumber.ifBlank { ac.tailNumber }
                    val mergedOp = meta.operator.ifBlank { ac.registeredOperator }
                    val mergedModel = meta.model.ifBlank { ac.aircraftTypeName }
                    val isHeli = ac.isHelicopter || meta.isHelicopter
                    next = next.copy(
                        tailNumber = mergedTail,
                        aircraftTypeCode = mergedType,
                        aircraftTypeName = mergedModel,
                        registeredOperator = mergedOp,
                        craftCategory = if (isHeli) CraftCategory.HELICOPTER else ac.craftCategory,
                        publicRegistry = AircraftRegistry.resolvePublicRegistry(mergedTail, ac.callsign, ac.icao24, mergedOp)
                    )
                }
                if (route != null) {
                    next = next.copy(flightPlan = route)
                }
                next
            }
            if (scanEpoch.get() == epoch) {
                _aircraftList.value = updated
                cachedAircraftForWidget = updated
            }
        }
    }

    private suspend fun processWatchlistAndTelemetry(
        sorted: List<Aircraft>,
        watchMap: Map<String, WatchlistEntity>,
        center: RadarCenter,
        radius: Double,
        now: Long,
        pollResult: AdsbPollResult
    ) {
        var alertCount = 0
        sorted.forEach { aircraft ->
            if (aircraft.isWatchlisted) {
                alertCount++
                flightTrackDao.insertPoint(
                    FlightTrackPointEntity(
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
                    ?: watchMap[aircraft.callsign.uppercase()]
                    ?: watchMap[aircraft.icao24.uppercase()]
                val alertRadius = watchItem?.alertRadiusNm?.toDouble() ?: radius
                if (aircraft.distanceNm <= alertRadius) {
                    val lastAlertTime = recentAlertCooldown[aircraft.tailNumber] ?: 0L
                    if (now - lastAlertTime > 120_000) {
                        recentAlertCooldown[aircraft.tailNumber] = now
                        logTelemetry(
                            "🚨 ALERT: ${aircraft.tailNumber} breached ${alertRadius.toInt()} NM radius! Range: ${
                                String.format(Locale.US, "%.1f", aircraft.distanceNm)
                            } NM"
                        )
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

        if (pollResult.isSuccess && sorted.isNotEmpty()) {
            val closest = sorted.first()
            val timeStr = UTC_FORMAT.format(Date(now))
            logTelemetry(
                "[$timeStr] ${pollResult.source.uppercase()} (${sorted.size}): ${closest.icao24.uppercase()} | ${closest.displayId} | ${closest.formattedAltitude} | ${closest.velocityKts}kts | ${closest.aircraftTypeCode}"
            )
        }
    }

    private fun logTelemetry(msg: String) {
        val current = _telemetryFeed.value.toMutableList()
        current.add(0, msg)
        _telemetryFeed.value = if (current.size > 50) current.take(50) else current
    }

    private fun calculateDistanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c * 0.539957
    }
}
