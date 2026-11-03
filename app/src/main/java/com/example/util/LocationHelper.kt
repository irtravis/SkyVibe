package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. Try to get best recent last known location
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLastKnown: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLastKnown == null || loc.time > bestLastKnown.time) {
                            bestLastKnown = loc
                        }
                    }
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        if (bestLastKnown != null && (System.currentTimeMillis() - bestLastKnown.time) < 120_000) {
            return bestLastKnown
        }

        // 2. Request single fresh update if last known is stale or null
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            continuation.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}
            }

            var requested = false
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                        requested = true
                        break
                    }
                } catch (_: SecurityException) {
                } catch (_: Exception) {
                }
            }

            if (!requested) {
                if (continuation.isActive) {
                    continuation.resume(bestLastKnown)
                }
            }
        }
    }
}
