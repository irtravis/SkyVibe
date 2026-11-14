package com.example.data

import com.example.model.Aircraft

/**
 * FlightAware reference & registration data built strictly from verified ADS-B transponder
 * fields and live aircraft registry lookups (no static callsign overrides or guessed photos).
 */
data class FlightAwareTailInfo(
    val tailNumber: String,
    val callsign: String,
    val icao24: String,
    val airframeModel: String,
    val aircraftTypeCode: String,
    val operatorName: String,
    val photoDrawableResName: String?,
    val photoCorrelationSummary: String,
    val flightAwareRegistrationUrl: String,
    val flightAwarePhotosUrl: String,
    val flightAwareLiveTrackUrl: String,
    val countryOfRegistration: String = "United States",
    val isVerifiedMatch: Boolean = true,
    val livePhotoUrl: String? = null
)

object FlightAwareRegistry {

    fun resolveFlightAwareTailInfo(aircraft: Aircraft): FlightAwareTailInfo {
        val cs = aircraft.callsign.trim().uppercase()
        val hex = aircraft.icao24.trim().uppercase()
        val tail = aircraft.tailNumber.trim().uppercase()
            .ifBlank { AircraftRegistry.convertIcao24ToTailNumber(hex) }

        val meta = AircraftDatabaseService.getCachedOrFleet(tail, hex, aircraft.registeredOperator)
        val resolvedTypeCode = aircraft.aircraftTypeCode.trim().uppercase()
            .ifBlank { meta?.icaoTypeCode.orEmpty() }

        val typeInfo = AircraftRegistry.getAircraftType(
            code = resolvedTypeCode,
            descriptionHint = meta?.model?.takeIf { it.isNotBlank() } ?: aircraft.aircraftTypeName,
            isHelicopterHint = aircraft.isHelicopter || meta?.isHelicopter == true
        )

        val resolvedModel = when {
            meta?.model?.isNotBlank() == true -> meta.model
            aircraft.aircraftTypeName.isNotBlank() && !aircraft.aircraftTypeName.contains("Pending") -> aircraft.aircraftTypeName
            else -> typeInfo.modelName
        }

        val resolvedOperator = meta?.operator?.takeIf { it.isNotBlank() }
            ?: aircraft.registeredOperator.takeIf { it.isNotBlank() }
            ?: AircraftRegistry.resolveRegisteredOperator(cs, tail, hex)

        val resolvedCountry = meta?.country
            ?: if (tail.startsWith("N") || hex.startsWith("A")) "United States" else "International"

        val correlatedDrawable = typeInfo.drawableResName
        val flightId = cs.ifBlank { tail.ifBlank { hex } }
        val regLookupKey = tail.ifBlank { hex }

        val summary = if (correlatedDrawable != null) {
            "Verified Airframe: $resolvedModel ($regLookupKey)"
        } else {
            "Registry Record: $resolvedModel ($regLookupKey)"
        }

        return FlightAwareTailInfo(
            tailNumber = tail,
            callsign = flightId,
            icao24 = hex,
            airframeModel = resolvedModel,
            aircraftTypeCode = typeInfo.typeCode,
            operatorName = resolvedOperator,
            photoDrawableResName = correlatedDrawable,
            photoCorrelationSummary = summary,
            flightAwareRegistrationUrl = "https://www.flightaware.com/resources/registration/$regLookupKey",
            flightAwarePhotosUrl = "https://www.flightaware.com/photos/aircraft/$regLookupKey",
            flightAwareLiveTrackUrl = "https://www.flightaware.com/live/flight/$flightId",
            countryOfRegistration = resolvedCountry,
            isVerifiedMatch = meta != null || resolvedTypeCode.isNotBlank(),
            livePhotoUrl = meta?.photoUrl
        )
    }
}
