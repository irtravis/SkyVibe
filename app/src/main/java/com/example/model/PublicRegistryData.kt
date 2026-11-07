package com.example.model

/**
 * Official civil aviation public registry data (FAA Civil Aviation Registry / ICAO).
 * Reflects public records for registered owner, registered party / representative, status, and jurisdiction.
 */
data class PublicRegistryData(
    val tailNumber: String,
    val icao24: String,
    val registeredOwner: String,
    val registeredParty: String,
    val representativeTitle: String = "Authorized Representative / Registered Agent",
    val partyClassification: String = "Corporation",
    val registryAuthority: String = "Federal Aviation Administration (FAA)",
    val registryCountry: String = "United States",
    val registryStateOrProvince: String? = null,
    val certificateStatus: String = "Valid (Active Certificate of Aircraft Registration)",
    val airworthinessClass: String = "Standard Airworthiness",
    val serialNumber: String = "",
    val issueDate: String = "",
    val expirationDate: String = ""
)
