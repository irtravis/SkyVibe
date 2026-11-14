package com.example.data

import android.util.Log
import com.example.model.Aircraft
import com.example.model.FlightPlanData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Verified aircraft metadata resolved directly from live ADS-B fields (`t`, `desc`, `ownOp`, `r`),
 * public aircraft registries (ADSBDB / HexDB), or deterministic FAA airline fleet blocks.
 */
data class AircraftMetadata(
    val tailNumber: String,
    val icao24: String,
    val manufacturer: String,
    val model: String,
    val icaoTypeCode: String,
    val operator: String,
    val country: String = "United States",
    val photoUrl: String? = null,
    val photoThumbnailUrl: String? = null,
    val isLiveVerified: Boolean = true
) {
    val isAirbus: Boolean
        get() = manufacturer.contains("Airbus", ignoreCase = true) && !isHelicopter ||
                (!isHelicopter && icaoTypeCode.uppercase() in setOf(
                    "A318", "A319", "A320", "A20N", "A321", "A21N",
                    "A332", "A333", "A339", "A359", "A35K", "A388",
                    "A220", "BCS1", "BCS3"
                ))

    val isBoeing: Boolean
        get() = manufacturer.contains("Boeing", ignoreCase = true) ||
                model.contains("Boeing", ignoreCase = true) ||
                icaoTypeCode.uppercase() in setOf(
                    "B712", "B737", "B738", "B739", "B38M", "B39M",
                    "B744", "B748", "B752", "B753", "B763", "B764",
                    "B772", "B77L", "B77W", "B788", "B789", "B78X"
                )

    val isHelicopter: Boolean
        get() = manufacturer.contains("Bell", ignoreCase = true) ||
                manufacturer.contains("Sikorsky", ignoreCase = true) ||
                manufacturer.contains("Airbus Helicopters", ignoreCase = true) ||
                manufacturer.contains("Eurocopter", ignoreCase = true) ||
                manufacturer.contains("Robinson", ignoreCase = true) ||
                manufacturer.contains("Leonardo", ignoreCase = true) ||
                model.contains("Helicopter", ignoreCase = true) ||
                icaoTypeCode.uppercase() in Aircraft.ROTORCRAFT_ICAO_TYPES

    val correlatedPhotoDrawable: String?
        get() = AircraftRegistry.getAircraftType(
            code = icaoTypeCode,
            descriptionHint = model,
            isHelicopterHint = isHelicopter
        ).drawableResName
}

/**
 * Live Aircraft & Flight Route Database Service.
 * Strictly avoids pre-seeding fake flights or guessing aircraft manufacturers.
 */
object AircraftDatabaseService {

    private const val TAG = "AircraftDatabaseService"
    private const val USER_AGENT = "SkyWatch-ADS-B/17.0"
    private const val NEGATIVE_CACHE_TTL_MS = 10 * 60 * 1000L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // In-memory aircraft cache keyed by uppercase registration or uppercase ICAO24 hex
    private val aircraftCache = ConcurrentHashMap<String, AircraftMetadata>()
    private val failedAircraftLookups = ConcurrentHashMap<String, Long>()

    // Live flight route cache keyed by uppercase trimmed callsign
    private data class CachedRoute(val route: FlightPlanData?, val timestampMs: Long)
    private val routeCache = ConcurrentHashMap<String, CachedRoute>()

    /**
     * Records authentic aircraft details directly from a live readsb v2 ADS-B target frame.
     */
    fun recordFromLiveAdsb(target: LiveAdsbTarget) {
        val cleanHex = target.icao24.trim().uppercase()
        val cleanTail = target.registration.trim().uppercase()
        val cleanType = target.icaoTypeCode.trim().uppercase()
        val cleanDesc = target.description.trim()
        val cleanOwner = target.registeredOwner.trim()

        if (cleanType.isBlank() && cleanDesc.isBlank() && cleanOwner.isBlank()) return

        val typeInfo = AircraftRegistry.getAircraftType(
            code = cleanType,
            descriptionHint = cleanDesc,
            isHelicopterHint = target.category.equals("A7", ignoreCase = true)
        )
        val manufacturer = if (typeInfo.manufacturer != "Pending Registry Lookup") typeInfo.manufacturer else ""
        val model = cleanDesc.ifBlank { if (cleanType.isNotBlank()) typeInfo.modelName else "" }
        if (model.isBlank() && manufacturer.isBlank() && cleanType.isBlank()) return

        val meta = AircraftMetadata(
            tailNumber = cleanTail,
            icao24 = cleanHex,
            manufacturer = manufacturer,
            model = model,
            icaoTypeCode = cleanType,
            operator = cleanOwner,
            country = if (cleanTail.startsWith("N") || cleanHex.startsWith("A")) "United States" else "International",
            isLiveVerified = true
        )
        cacheAircraftResult(meta)
    }

    /**
     * Synchronous lookup against live in-memory cache and deterministic US airline tail suffix patterns.
     * Never guesses unknown tail numbers as Boeing/Airbus.
     */
    fun getCachedOrFleet(tailNumber: String, icao24: String, operatorHint: String = ""): AircraftMetadata? {
        val cleanTail = tailNumber.trim().uppercase()
        val cleanHex = icao24.trim().uppercase()

        if (cleanTail.isNotEmpty()) aircraftCache[cleanTail]?.let { return it }
        if (cleanHex.isNotEmpty()) aircraftCache[cleanHex]?.let { return it }

        val fleet = resolveOfflineFleet(cleanTail, cleanHex, operatorHint)
        if (fleet != null) {
            cacheAircraftResult(fleet)
            return fleet
        }
        return null
    }

    /**
     * Asynchronous live aircraft model & owner lookup against ADSBDB and HexDB.
     */
    suspend fun lookupAircraft(
        tailNumber: String,
        icao24: String,
        operatorHint: String = ""
    ): AircraftMetadata? = withContext(Dispatchers.IO) {
        val cleanTail = tailNumber.trim().uppercase()
        val cleanHex = icao24.trim().uppercase()

        if (cleanTail.isNotEmpty()) aircraftCache[cleanTail]?.let { return@withContext it }
        if (cleanHex.isNotEmpty()) aircraftCache[cleanHex]?.let { return@withContext it }

        val lookupKey = cleanHex.ifEmpty { cleanTail }
        if (lookupKey.isEmpty()) return@withContext null

        val lastFail = failedAircraftLookups[lookupKey]
        if (lastFail != null && System.currentTimeMillis() - lastFail < NEGATIVE_CACHE_TTL_MS) {
            return@withContext resolveOfflineFleet(cleanTail, cleanHex, operatorHint)
        }

        // 1. Query ADSBDB by Mode-S hex first (globally unique), then tail registration
        val adsbdbResult = (if (cleanHex.isNotEmpty()) queryAdsbDb(cleanHex) else null)
            ?: (if (cleanTail.isNotEmpty()) queryAdsbDb(cleanTail) else null)

        if (adsbdbResult != null) {
            cacheAircraftResult(adsbdbResult)
            return@withContext adsbdbResult
        }

        // 2. Fallback to HexDB v2
        val hexdbResult = queryHexDb(cleanTail, cleanHex)
        if (hexdbResult != null) {
            cacheAircraftResult(hexdbResult)
            return@withContext hexdbResult
        }

        failedAircraftLookups[lookupKey] = System.currentTimeMillis()

        // 3. Deterministic airline tail suffix fallback only
        val offline = resolveOfflineFleet(cleanTail, cleanHex, operatorHint)
        if (offline != null) {
            cacheAircraftResult(offline)
            return@withContext offline
        }

        null
    }

    fun getCachedRoute(callsign: String): FlightPlanData? {
        val clean = callsign.trim().uppercase()
        if (clean.length < 4) return null
        return routeCache[clean]?.route
    }

    /**
     * Queries live verified flight route (Origin -> Destination) by transponder callsign
     * from ADSBDB and HexDB. Returns null when no verified live route exists (never guesses).
     */
    suspend fun lookupLiveRoute(callsign: String): FlightPlanData? = withContext(Dispatchers.IO) {
        val clean = callsign.trim().uppercase().replace(" ", "")
        if (clean.length < 4) return@withContext null

        val existing = routeCache[clean]
        if (existing != null) {
            if (existing.route != null || System.currentTimeMillis() - existing.timestampMs < NEGATIVE_CACHE_TTL_MS) {
                return@withContext existing.route
            }
        }

        val fetched = queryAdsbDbRoute(clean) ?: queryHexDbRoute(clean)
        routeCache[clean] = CachedRoute(fetched, System.currentTimeMillis())
        fetched
    }

    private fun cacheAircraftResult(meta: AircraftMetadata) {
        if (meta.tailNumber.isNotBlank()) aircraftCache[meta.tailNumber.uppercase()] = meta
        if (meta.icao24.isNotBlank()) aircraftCache[meta.icao24.uppercase()] = meta
    }

    private fun queryAdsbDb(key: String): AircraftMetadata? {
        return try {
            val request = Request.Builder()
                .url("https://api.adsbdb.com/v0/aircraft/$key")
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val resp = root.optJSONObject("response") ?: return null
                val ac = resp.optJSONObject("aircraft") ?: return null

                val manufacturer = ac.optString("manufacturer", "").trim()
                val type = ac.optString("type", "").trim()
                val icaoType = ac.optString("icao_type", "").trim().uppercase()
                val modeS = ac.optString("mode_s", "").trim().uppercase()
                val reg = ac.optString("registration", "").trim().uppercase()
                val owner = ac.optString("registered_owner", "").trim()
                val country = ac.optString("registered_owner_country_name", "").trim()
                    .ifBlank { if (reg.startsWith("N")) "United States" else "International" }
                val photo = ac.optString("url_photo", "").takeIf { it.isNotBlank() && it != "null" }
                val photoThumb = ac.optString("url_photo_thumbnail", "").takeIf { it.isNotBlank() && it != "null" }

                if (manufacturer.isBlank() && type.isBlank() && icaoType.isBlank()) return null

                val modelStr = when {
                    type.isNotBlank() && manufacturer.isNotBlank() && !type.startsWith(manufacturer, ignoreCase = true) -> "$manufacturer $type"
                    type.isNotBlank() -> type
                    icaoType.isNotBlank() -> AircraftRegistry.getAircraftType(icaoType).modelName
                    else -> manufacturer
                }

                AircraftMetadata(
                    tailNumber = reg.ifBlank { if (!key.matches(Regex("[0-9A-F]{6}"))) key else "" },
                    icao24 = modeS.ifBlank { if (key.matches(Regex("[0-9A-F]{6}"))) key else "" },
                    manufacturer = manufacturer.ifBlank { AircraftRegistry.getAircraftType(icaoType, modelStr).manufacturer },
                    model = modelStr,
                    icaoTypeCode = icaoType,
                    operator = owner,
                    country = country,
                    photoUrl = photo,
                    photoThumbnailUrl = photoThumb,
                    isLiveVerified = true
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "ADSBDB lookup error for $key: ${e.message}")
            null
        }
    }

    private fun queryHexDb(tail: String, hex: String): AircraftMetadata? {
        return try {
            val url = if (hex.isNotEmpty()) {
                "https://hexdb.io/api/v2/aircraft/$hex"
            } else {
                "https://hexdb.io/api/v2/aircraft?registration=$tail"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val obj = if (hex.isNotEmpty()) {
                    JSONObject(body)
                } else {
                    val root = JSONObject(body)
                    val results = root.optJSONArray("results") ?: return null
                    if (results.length() == 0) return null
                    results.getJSONObject(0)
                }

                val manufacturer = obj.optString("manufacturer", "").trim()
                val type = obj.optString("type", "").trim()
                val icaoType = obj.optString("icao_type_code", "").trim().uppercase()
                val reg = obj.optString("registration", tail).trim().uppercase()
                val hexCode = obj.optString("hex", hex).trim().uppercase()
                val owner = obj.optString("registered_owner", "").trim()

                if (manufacturer.isBlank() && type.isBlank() && icaoType.isBlank()) return null

                val modelStr = when {
                    type.isNotBlank() && manufacturer.isNotBlank() && !type.startsWith(manufacturer, ignoreCase = true) -> "$manufacturer $type"
                    type.isNotBlank() -> type
                    icaoType.isNotBlank() -> AircraftRegistry.getAircraftType(icaoType).modelName
                    else -> manufacturer
                }

                AircraftMetadata(
                    tailNumber = reg,
                    icao24 = hexCode,
                    manufacturer = manufacturer.ifBlank { AircraftRegistry.getAircraftType(icaoType, modelStr).manufacturer },
                    model = modelStr,
                    icaoTypeCode = icaoType,
                    operator = owner,
                    country = if (reg.startsWith("N") || hexCode.startsWith("A")) "United States" else "International",
                    isLiveVerified = true
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "HexDB lookup error: ${e.message}")
            null
        }
    }

    private fun queryAdsbDbRoute(callsign: String): FlightPlanData? {
        return try {
            val request = Request.Builder()
                .url("https://api.adsbdb.com/v0/callsign/$callsign")
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val resp = root.optJSONObject("response") ?: return null
                val fr = resp.optJSONObject("flightroute") ?: return null
                val orig = fr.optJSONObject("origin") ?: return null
                val dest = fr.optJSONObject("destination") ?: return null
                val airline = fr.optJSONObject("airline")

                val origIcao = orig.optString("icao_code", "").ifBlank { orig.optString("iata_code", "") }.uppercase()
                val destIcao = dest.optString("icao_code", "").ifBlank { dest.optString("iata_code", "") }.uppercase()
                if (origIcao.isBlank() || destIcao.isBlank()) return null

                FlightPlanData(
                    flightNumber = fr.optString("callsign", callsign).ifBlank { callsign },
                    originIcao = origIcao,
                    originName = orig.optString("name", origIcao).ifBlank { origIcao },
                    originCity = orig.optString("municipality", origIcao).ifBlank { origIcao },
                    destinationIcao = destIcao,
                    destinationName = dest.optString("name", destIcao).ifBlank { destIcao },
                    destinationCity = dest.optString("municipality", destIcao).ifBlank { destIcao },
                    airlineName = airline?.optString("name", "").orEmpty(),
                    sourceName = "ADSBDB Live Route",
                    isLiveVerified = true
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "ADSBDB route lookup error for $callsign: ${e.message}")
            null
        }
    }

    private fun queryHexDbRoute(callsign: String): FlightPlanData? {
        return try {
            val request = Request.Builder()
                .url("https://hexdb.io/api/v1/route/icao/$callsign")
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val routeStr = root.optString("route", "").trim().uppercase()
                if (!routeStr.contains("-")) return null

                val parts = routeStr.split("-").map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size < 2) return null

                val origCode = parts.first()
                val destCode = parts.last()
                val origInfo = AirportDatabase.findAirportInfo(origCode)
                val destInfo = AirportDatabase.findAirportInfo(destCode)

                FlightPlanData(
                    flightNumber = callsign,
                    originIcao = origInfo?.icao ?: origCode,
                    originName = origInfo?.name ?: origCode,
                    originCity = origInfo?.city ?: origCode,
                    destinationIcao = destInfo?.icao ?: destCode,
                    destinationName = destInfo?.name ?: destCode,
                    destinationCity = destInfo?.city ?: destCode,
                    airlineName = "",
                    sourceName = "HexDB Live Route",
                    isLiveVerified = true
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "HexDB route lookup error for $callsign: ${e.message}")
            null
        }
    }

    /**
     * Deterministic FAA airline fleet registration blocks ONLY.
     * Returns null for ambiguous/unverified tail numbers so we never guess Boeing vs Airbus.
     */
    fun resolveOfflineFleet(tail: String, hex: String, operatorHint: String = ""): AircraftMetadata? {
        val cleanTail = tail.trim().uppercase()
        if (cleanTail.isBlank() || !cleanTail.startsWith("N")) return null

        val (mfg, model, typeCode, op) = when {
            // Delta Air Lines specific deterministic sub-fleets
            cleanTail.matches(Regex("N3[0-9]{2}DN")) ->
                Quad("Airbus", "Airbus A321-211", "A321", "Delta Air Lines")
            cleanTail.matches(Regex("N1[0-9]{2}DU")) || cleanTail.matches(Regex("N3[0-9]{2}DU")) ->
                Quad("Airbus", "Airbus A220-300", "BCS3", "Delta Air Lines")
            cleanTail.matches(Regex("N5[0-9]{2}DN")) ->
                Quad("Airbus", "Airbus A350-900", "A359", "Delta Air Lines")
            cleanTail.matches(Regex("N4[0-9]{2}DX")) ->
                Quad("Airbus", "Airbus A330-900neo", "A339", "Delta Air Lines")
            cleanTail.matches(Regex("N3[0-7][0-9]NW")) ->
                Quad("Airbus", "Airbus A320-200", "A320", "Delta Air Lines")
            cleanTail.matches(Regex("N3[0-7][0-9]NB")) ->
                Quad("Airbus", "Airbus A319-100", "A319", "Delta Air Lines")
            cleanTail.matches(Regex("N8[0-9]{2}DN")) || cleanTail.matches(Regex("N9[0-2][0-9]DN")) ->
                Quad("Boeing", "Boeing 737-900ER", "B739", "Delta Air Lines")

            // Frontier Airlines (all-Airbus A320/A321 fleet)
            cleanTail.matches(Regex("N7[0-9]{2}FR")) || cleanTail.matches(Regex("N6[0-9]{2}FR")) ->
                Quad("Airbus", "Airbus A321neo", "A21N", "Frontier Airlines")
            cleanTail.matches(Regex("N3[0-9]{2}FR")) || cleanTail.matches(Regex("N2[0-9]{2}FR")) ->
                Quad("Airbus", "Airbus A320neo", "A20N", "Frontier Airlines")

            // Spirit Airlines (all-Airbus fleet)
            cleanTail.matches(Regex("N6[0-9]{2}NK")) ->
                Quad("Airbus", "Airbus A321-200", "A321", "Spirit Airlines")
            cleanTail.matches(Regex("N9[0-9]{2}NK")) ->
                Quad("Airbus", "Airbus A320neo", "A20N", "Spirit Airlines")

            // Southwest Airlines (all-Boeing 737 fleet)
            cleanTail.matches(Regex("N8[0-9]{2}WN")) ->
                Quad("Boeing", "Boeing 737-800 / MAX 8", "B738", "Southwest Airlines")
            cleanTail.matches(Regex("N[2479][0-9]{2}WN")) ->
                Quad("Boeing", "Boeing 737-700", "B737", "Southwest Airlines")

            else -> return null
        }

        return AircraftMetadata(
            tailNumber = cleanTail,
            icao24 = hex.trim().uppercase(),
            manufacturer = mfg,
            model = model,
            icaoTypeCode = typeCode,
            operator = operatorHint.ifBlank { op },
            country = "United States",
            isLiveVerified = true
        )
    }

    private data class Quad(
        val first: String,
        val second: String,
        val third: String,
        val fourth: String
    )
}
