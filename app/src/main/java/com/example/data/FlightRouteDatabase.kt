package com.example.data

import com.example.model.Aircraft
import com.example.model.FlightPlanData

/**
 * Live Flight Route Facade.
 * Resolves only verified origin -> destination itineraries cached or fetched from live
 * ADSBDB / HexDB APIs. Never guesses routes from compass headings or static tables.
 */
object FlightRouteDatabase {

    fun getCachedRoute(callsign: String): FlightPlanData? {
        return AircraftDatabaseService.getCachedRoute(callsign)
    }

    fun resolveFlightPlan(aircraft: Aircraft): FlightPlanData? {
        return aircraft.flightPlan ?: AircraftDatabaseService.getCachedRoute(aircraft.callsign)
    }

    suspend fun resolveFlightPlanAsync(callsign: String): FlightPlanData? {
        return AircraftDatabaseService.lookupLiveRoute(callsign)
    }
}
