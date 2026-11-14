package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Raw verified ADS-B transponder contact parsed directly from live feeds
 * (adsb.lol, airplanes.live, adsb.fi, or OpenSky Network).
 */
data class LiveAdsbTarget(
    val icao24: String,
    val callsign: String,
    val registration: String,
    val icaoTypeCode: String,
    val description: String,
    val registeredOwner: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeFt: Int,
    val onGround: Boolean,
    val velocityKts: Int,
    val headingDeg: Float,
    val verticalRateFpm: Int,
    val squawk: String,
    val seenSec: Double,
    val seenPosSec: Double,
    val sourceFeed: String
)

data class AdsbPollResult(
    val targets: List<LiveAdsbTarget>,
    val source: String,
    val timestampMs: Long,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = errorMessage == null
}

/**
 * Clean, hallucination-free live ADS-B polling engine.
 * Queries community readsb v2 aggregators (adsb.lol, airplanes.live, adsb.fi)
 * with OpenSky Network as a rate-limited backup. Filters out ground targets
 * and stale contacts so landed or out-of-range aircraft disappear promptly.
 */
object AdsBApiService {

    private const val TAG = "AdsBApiService"
    private const val MAX_SEEN_POS_SEC = 45.0
    private const val MAX_SEEN_SEC = 60.0
    private const val OPENSKY_COOLDOWN_MS = 20_000L

    @Volatile
    private var openSkyCooldownUntilMs: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLiveAirspace(
        centerLat: Double,
        centerLon: Double,
        radiusNm: Double
    ): AdsbPollResult = withContext(Dispatchers.IO) {
        val distNm = radiusNm.roundToInt().coerceIn(5, 250)
        val latStr = String.format(Locale.US, "%.5f", centerLat)
        val lonStr = String.format(Locale.US, "%.5f", centerLon)

        val v2Endpoints = listOf(
            "adsb.lol" to "https://api.adsb.lol/v2/lat/$latStr/lon/$lonStr/dist/$distNm",
            "airplanes.live" to "https://api.airplanes.live/v2/point/$latStr/$lonStr/$distNm",
            "adsb.fi" to "https://opendata.adsb.fi/api/v2/lat/$latStr/lon/$lonStr/dist/$distNm"
        )

        var lastError = "No ADS-B feeds reachable"

        for ((feedName, url) in v2Endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SkyWatch-ADS-B/17.0")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val parsed = parseReadsbV2Json(body, feedName)
                            return@withContext AdsbPollResult(
                                targets = parsed,
                                source = feedName,
                                timestampMs = System.currentTimeMillis()
                            )
                        }
                        lastError = "$feedName returned empty body"
                    } else {
                        lastError = "$feedName HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "$feedName poll error: ${e.message}")
                lastError = "$feedName: ${e.javaClass.simpleName}"
            }
        }

        // OpenSky Network fallback (only if all v2 feeds failed and cooldown elapsed)
        val nowMs = System.currentTimeMillis()
        if (nowMs >= openSkyCooldownUntilMs) {
            try {
                val latDelta = radiusNm / 60.0
                val lonDelta = radiusNm / (60.0 * cos(Math.toRadians(centerLat)).coerceAtLeast(0.15))
                val openSkyUrl = String.format(
                    Locale.US,
                    "https://opensky-network.org/api/states/all?lamin=%.4f&lomin=%.4f&lamax=%.4f&lomax=%.4f",
                    centerLat - latDelta,
                    centerLon - lonDelta,
                    centerLat + latDelta,
                    centerLon + lonDelta
                )
                val request = Request.Builder()
                    .url(openSkyUrl)
                    .header("User-Agent", "SkyWatch-ADS-B/17.0")
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        openSkyCooldownUntilMs = System.currentTimeMillis() + OPENSKY_COOLDOWN_MS
                        lastError = "OpenSky HTTP 429 (rate limited)"
                    } else if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val parsed = parseOpenSkyJson(body, nowMs / 1000L)
                            return@withContext AdsbPollResult(
                                targets = parsed,
                                source = "opensky-network.org",
                                timestampMs = nowMs
                            )
                        }
                    } else {
                        lastError = "OpenSky HTTP ${response.code}"
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "OpenSky poll error: ${e.message}")
                lastError = "OpenSky: ${e.javaClass.simpleName}"
            }
        }

        AdsbPollResult(
            targets = emptyList(),
            source = "offline",
            timestampMs = System.currentTimeMillis(),
            errorMessage = lastError
        )
    }

    /**
     * Parses readsb / tar1090 v2 JSON responses (used by adsb.lol, airplanes.live, and adsb.fi).
     * Filters out landed ("alt_baro": "ground"), ground vehicles ("C*"), and stale (>45s) contacts.
     */
    fun parseReadsbV2Json(jsonString: String, sourceFeed: String = "adsb.lol"): List<LiveAdsbTarget> {
        val root = JSONObject(jsonString)
        val acArray = root.optJSONArray("ac") ?: root.optJSONArray("aircraft") ?: return emptyList()
        val results = mutableListOf<LiveAdsbTarget>()

        for (i in 0 until acArray.length()) {
            val item = acArray.optJSONObject(i) ?: continue
            val hex = item.optString("hex", "").trim().lowercase()
            if (hex.isBlank()) continue

            if (!item.has("lat") || !item.has("lon") || item.isNull("lat") || item.isNull("lon")) continue
            val lat = item.optDouble("lat", Double.NaN)
            val lon = item.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue

            // Exclude ground vehicles & surface obstacles (ADS-B Category C1..C3)
            val category = item.optString("category", "").trim().uppercase()
            if (category.startsWith("C")) continue

            // Reject stale aircraft whose transponder stopped reporting (e.g. landed / out of coverage)
            val seen = item.optDouble("seen", 0.0)
            val seenPos = item.optDouble("seen_pos", seen)
            if (seenPos > MAX_SEEN_POS_SEC || seen > MAX_SEEN_SEC) continue

            // Handle readsb mixed alt_baro: can be "ground" string or numeric feet
            val rawAltBaro = item.opt("alt_baro")
            val isBaroGround = rawAltBaro is String && rawAltBaro.equals("ground", ignoreCase = true)
            if (isBaroGround) continue

            val baroAltFt = when (rawAltBaro) {
                is Number -> rawAltBaro.toDouble().roundToInt()
                is String -> rawAltBaro.toDoubleOrNull()?.roundToInt()
                else -> null
            }
            val geomAltFt = if (item.has("alt_geom") && !item.isNull("alt_geom")) {
                item.optDouble("alt_geom", Double.NaN).takeIf { !it.isNaN() }?.roundToInt()
            } else null

            val altitudeFt = baroAltFt ?: geomAltFt ?: 0
            val gs = item.optDouble("gs", Double.NaN).takeIf { !it.isNaN() }?.roundToInt() ?: 0

            // Exclude parked/stationary aircraft on runway/apron
            if (altitudeFt <= 50 && gs < 30) continue

            val track = item.optDouble("track", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
                ?: item.optDouble("true_heading", Double.NaN).takeIf { !it.isNaN() }?.toFloat()
                ?: 0f

            val vertRate = item.optDouble("baro_rate", Double.NaN).takeIf { !it.isNaN() }?.roundToInt()
                ?: item.optDouble("geom_rate", Double.NaN).takeIf { !it.isNaN() }?.roundToInt()
                ?: 0

            val flight = item.optString("flight", "").trim().uppercase()
            val registration = item.optString("r", "").trim().uppercase()
            val typeCode = item.optString("t", "").trim().uppercase()
            val desc = item.optString("desc", "").trim()
            val ownOp = item.optString("ownOp", "").trim()
            val squawk = item.optString("squawk", "").trim().ifBlank { "1200" }

            results.add(
                LiveAdsbTarget(
                    icao24 = hex,
                    callsign = flight,
                    registration = registration,
                    icaoTypeCode = typeCode,
                    description = desc,
                    registeredOwner = ownOp,
                    category = category,
                    latitude = lat,
                    longitude = lon,
                    altitudeFt = altitudeFt.coerceAtLeast(0),
                    onGround = false,
                    velocityKts = gs.coerceAtLeast(0),
                    headingDeg = ((track % 360f) + 360f) % 360f,
                    verticalRateFpm = vertRate,
                    squawk = squawk,
                    seenSec = seen,
                    seenPosSec = seenPos,
                    sourceFeed = sourceFeed
                )
            )
        }

        return results
    }

    /**
     * Parses OpenSky /api/states/all JSON fallback with strict freshness & on_ground filtering.
     */
    fun parseOpenSkyJson(jsonString: String, referenceEpochSec: Long = System.currentTimeMillis() / 1000L): List<LiveAdsbTarget> {
        val root = JSONObject(jsonString)
        val serverTime = root.optLong("time", referenceEpochSec).takeIf { it > 0 } ?: referenceEpochSec
        val states: JSONArray = root.optJSONArray("states") ?: return emptyList()
        val results = mutableListOf<LiveAdsbTarget>()

        for (i in 0 until states.length()) {
            val row = states.optJSONArray(i) ?: continue
            val icao24 = row.optString(0, "").trim().lowercase()
            if (icao24.isBlank()) continue

            val onGround = row.optBoolean(8, false)
            if (onGround) continue

            val lon = row.optDouble(5, Double.NaN)
            val lat = row.optDouble(6, Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue

            val timePos = row.optLong(3, serverTime)
            val lastContact = row.optLong(4, serverTime)
            val posAgeSec = (serverTime - timePos).coerceAtLeast(0L).toDouble()
            val contactAgeSec = (serverTime - lastContact).coerceAtLeast(0L).toDouble()
            if (posAgeSec > MAX_SEEN_POS_SEC || contactAgeSec > MAX_SEEN_SEC) continue

            val baroMeters = row.optDouble(7, Double.NaN)
            val geoMeters = row.optDouble(13, Double.NaN)
            val altMeters = when {
                !baroMeters.isNaN() -> baroMeters
                !geoMeters.isNaN() -> geoMeters
                else -> 0.0
            }
            val altitudeFt = (altMeters * 3.28084).roundToInt().coerceAtLeast(0)

            val velocityMs = row.optDouble(9, 0.0).takeIf { !it.isNaN() } ?: 0.0
            val velocityKts = (velocityMs * 1.94384).roundToInt().coerceAtLeast(0)
            if (altitudeFt <= 50 && velocityKts < 30) continue

            val heading = row.optDouble(10, 0.0).takeIf { !it.isNaN() }?.toFloat() ?: 0f
            val vertRateMs = row.optDouble(11, 0.0).takeIf { !it.isNaN() } ?: 0.0
            val vertRateFpm = (vertRateMs * 196.85).roundToInt()

            val callsign = row.optString(1, "").trim().uppercase()
            val squawk = row.optString(14, "").trim().takeIf { it.isNotBlank() && it != "null" } ?: "1200"
            val emitterCatInt = row.optInt(17, 0)
            val categoryStr = when (emitterCatInt) {
                2 -> "A1"
                3 -> "A2"
                4 -> "A3"
                5 -> "A4"
                6 -> "A5"
                7 -> "A6"
                8 -> "A7"
                else -> ""
            }

            results.add(
                LiveAdsbTarget(
                    icao24 = icao24,
                    callsign = callsign,
                    registration = "",
                    icaoTypeCode = "",
                    description = "",
                    registeredOwner = "",
                    category = categoryStr,
                    latitude = lat,
                    longitude = lon,
                    altitudeFt = altitudeFt,
                    onGround = false,
                    velocityKts = velocityKts,
                    headingDeg = ((heading % 360f) + 360f) % 360f,
                    verticalRateFpm = vertRateFpm,
                    squawk = squawk,
                    seenSec = contactAgeSec,
                    seenPosSec = posAgeSec,
                    sourceFeed = "opensky-network.org"
                )
            )
        }

        return results
    }
}
