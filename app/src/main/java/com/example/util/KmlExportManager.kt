package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.Aircraft
import com.example.model.FlightTrackPointEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for exporting ADS-B telemetry, Watchlist flight tracks,
 * and live streaming layers to Google Earth in standard Keyhole Markup Language (KML 2.2).
 */
object KmlExportManager {

    private val UTC_ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Generates a 3D extruded KML document containing recorded flight paths, altitude profiles,
     * speeds, and public registry metadata for watchlist aircraft.
     */
    fun generateWatchlistTracksKml(
        tracks: List<FlightTrackPointEntity>,
        specificTail: String? = null
    ): String {
        val filtered = if (!specificTail.isNullOrBlank()) {
            tracks.filter { it.tailNumber.equals(specificTail, ignoreCase = true) }
        } else {
            tracks
        }

        val groupedByTail = filtered.groupBy { it.tailNumber.uppercase() }
        val sb = StringBuilder()

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>SkyWatch ADS-B Watchlist Flight Tracks</name>\n")
        sb.append("    <open>1</open>\n")
        sb.append("    <description>3D flight telemetry tracks recorded by SkyWatch ADS-B Receiver. Extruded to ground with true MSL altitude.</description>\n\n")

        // Styles for Fixed-Wing, Helicopters, and Watchlist Alert tracks
        sb.append("    <Style id=\"planeTrackStyle\">\n")
        sb.append("      <LineStyle>\n")
        sb.append("        <color>ff00e676</color>\n") // Green in aabbggrr
        sb.append("        <width>4</width>\n")
        sb.append("      </LineStyle>\n")
        sb.append("      <PolyStyle>\n")
        sb.append("        <color>4d00e676</color>\n") // Semi-transparent green curtain
        sb.append("      </PolyStyle>\n")
        sb.append("    </Style>\n\n")

        sb.append("    <Style id=\"heliTrackStyle\">\n")
        sb.append("      <LineStyle>\n")
        sb.append("        <color>ffffff00</color>\n") // Cyan
        sb.append("        <width>4</width>\n")
        sb.append("      </LineStyle>\n")
        sb.append("      <PolyStyle>\n")
        sb.append("        <color>4dffff00</color>\n")
        sb.append("      </PolyStyle>\n")
        sb.append("    </Style>\n\n")

        sb.append("    <Style id=\"aircraftPositionStyle\">\n")
        sb.append("      <IconStyle>\n")
        sb.append("        <scale>1.2</scale>\n")
        sb.append("        <Icon>\n")
        sb.append("          <href>http://maps.google.com/mapfiles/kml/shapes/airports.png</href>\n")
        sb.append("        </Icon>\n")
        sb.append("      </IconStyle>\n")
        sb.append("    </Style>\n\n")

        if (groupedByTail.isEmpty()) {
            sb.append("    <Folder>\n")
            sb.append("      <name>No Recorded Watchlist Tracks</name>\n")
            sb.append("      <description>Add tail numbers to the watchlist to record real-time breadcrumb routes.</description>\n")
            sb.append("    </Folder>\n")
        } else {
            for ((tail, points) in groupedByTail) {
                val sortedPoints = points.sortedBy { it.timestamp }
                val latestPoint = sortedPoints.lastOrNull() ?: continue
                val isHeli = latestPoint.aircraftType.contains("Helicopter", true) ||
                        latestPoint.aircraftType.contains("Rotorcraft", true) ||
                        latestPoint.callsign.startsWith("HELI") ||
                        latestPoint.callsign.startsWith("MED")

                val trackStyle = if (isHeli) "#heliTrackStyle" else "#planeTrackStyle"
                val craftGlyph = if (isHeli) "🚁" else "✈"

                sb.append("    <Folder>\n")
                sb.append("      <name>$craftGlyph $tail (${latestPoint.callsign})</name>\n")
                sb.append("      <description>\n")
                sb.append("        Operator: ${latestPoint.registeredOperator}&#10;\n")
                sb.append("        Craft Type: ${latestPoint.aircraftType}&#10;\n")
                sb.append("        Recorded Points: ${sortedPoints.size}&#10;\n")
                sb.append("        Peak Altitude: ${sortedPoints.maxOf { it.altitudeFt }} ft&#10;\n")
                sb.append("        Max Groundspeed: ${sortedPoints.maxOf { it.velocityKts }} kts&#10;\n")
                sb.append("      </description>\n\n")

                // 3D Extruded Flight Path LineString
                sb.append("      <Placemark>\n")
                sb.append("        <name>3D Flight Route Profile: $tail</name>\n")
                sb.append("        <styleUrl>$trackStyle</styleUrl>\n")
                sb.append("        <LineString>\n")
                sb.append("          <extrude>1</extrude>\n")
                sb.append("          <tessellate>1</tessellate>\n")
                sb.append("          <altitudeMode>absolute</altitudeMode>\n")
                sb.append("          <coordinates>\n")

                for (pt in sortedPoints) {
                    val altMeters = (pt.altitudeFt * 0.3048).toInt()
                    sb.append("            ${String.format(Locale.US, "%.5f", pt.longitude)},${String.format(Locale.US, "%.5f", pt.latitude)},$altMeters\n")
                }

                sb.append("          </coordinates>\n")
                sb.append("        </LineString>\n")
                sb.append("      </Placemark>\n\n")

                // Latest Real-Time Aircraft Fix
                val latestAltMeters = (latestPoint.altitudeFt * 0.3048).toInt()
                sb.append("      <Placemark>\n")
                sb.append("        <name>$tail Latest ADS-B Position</name>\n")
                sb.append("        <description><![CDATA[\n")
                sb.append("          <h3>$craftGlyph $tail • ${latestPoint.callsign}</h3>\n")
                sb.append("          <b>Craft Type:</b> ${latestPoint.aircraftType}<br/>\n")
                sb.append("          <b>Registered Operator:</b> ${latestPoint.registeredOperator}<br/>\n")
                sb.append("          <b>Altitude:</b> ${latestPoint.altitudeFt} ft MSL<br/>\n")
                sb.append("          <b>Ground Speed:</b> ${latestPoint.velocityKts} kts<br/>\n")
                sb.append("          <b>Heading:</b> ${latestPoint.headingDeg.toInt()}°<br/>\n")
                sb.append("          <b>Vertical Rate:</b> ${latestPoint.verticalRateFpm} fpm<br/>\n")
                sb.append("          <b>Fix Timestamp:</b> ${UTC_ISO.format(Date(latestPoint.timestamp))}<br/>\n")
                sb.append("        ]]></description>\n")
                sb.append("        <styleUrl>#aircraftPositionStyle</styleUrl>\n")
                sb.append("        <Point>\n")
                sb.append("          <altitudeMode>absolute</altitudeMode>\n")
                sb.append("          <coordinates>${String.format(Locale.US, "%.5f", latestPoint.longitude)},${String.format(Locale.US, "%.5f", latestPoint.latitude)},$latestAltMeters</coordinates>\n")
                sb.append("        </Point>\n")
                sb.append("      </Placemark>\n")

                sb.append("    </Folder>\n\n")
            }
        }

        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    /**
     * Generates a Google Earth NetworkLink KML for real-time live data streaming.
     * Google Earth will periodically auto-refresh the layer to display updated ADS-B aircraft positions.
     */
    fun generateLiveStreamingNetworkLinkKml(
        targetFileName: String = "skywatch_live_stream.kml",
        refreshIntervalSec: Int = 5
    ): String {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <NetworkLink>
    <name>SkyWatch ADS-B Live Airspace Streaming Layer</name>
    <open>1</open>
    <description>Live dynamic ADS-B 1090MHz transponder feed for Google Earth. Auto-refreshes every $refreshIntervalSec seconds with current aircraft and helicopter radar targets.</description>
    <flyToView>1</flyToView>
    <Link>
      <href>$targetFileName</href>
      <refreshMode>onInterval</refreshMode>
      <refreshInterval>$refreshIntervalSec</refreshInterval>
    </Link>
  </NetworkLink>
</kml>
""".trimIndent()
    }

    /**
     * Generates an instantaneous 3D KML snapshot of all live aircraft currently in the radar coverage.
     */
    fun generateLiveAirspaceSnapshotKml(
        aircraftList: List<Aircraft>,
        centerName: String
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>SkyWatch Live Airspace: $centerName</name>\n")
        sb.append("    <description>Real-time ADS-B transponder snapshot captured at ${UTC_ISO.format(Date())}</description>\n\n")

        for (ac in aircraftList) {
            val glyph = if (ac.isHelicopter) "🚁" else "✈"
            val altMeters = (ac.altitudeFt * 0.3048).toInt()
            val regOwner = ac.publicRegistry?.registeredOwner ?: ac.registeredOperator
            val repParty = ac.publicRegistry?.registeredParty ?: "Authorized Representative"

            sb.append("    <Placemark>\n")
            sb.append("      <name>$glyph ${ac.displayId}</name>\n")
            sb.append("      <description><![CDATA[\n")
            sb.append("        <h3>$glyph ${ac.tailNumber} (${ac.callsign})</h3>\n")
            sb.append("        <b>Type:</b> ${ac.aircraftTypeName} (${ac.aircraftTypeCode})<br/>\n")
            sb.append("        <b>Category:</b> ${if (ac.isHelicopter) "Helicopter / Rotorcraft" else "Airplane"}<br/>\n")
            sb.append("        <b>Registered Owner:</b> $regOwner<br/>\n")
            sb.append("        <b>Representative / Agent:</b> $repParty<br/>\n")
            sb.append("        <b>Altitude:</b> ${ac.formattedAltitude} (${ac.altitudeFt} ft)<br/>\n")
            sb.append("        <b>Speed:</b> ${ac.velocityKts} kts • <b>Track:</b> ${ac.headingDeg.toInt()}°<br/>\n")
            sb.append("        <b>Squawk:</b> ${ac.squawk} • <b>Range:</b> ${String.format(Locale.US, "%.1f", ac.distanceNm)} NM<br/>\n")
            if (ac.nearestStormName != null) {
                sb.append("        <b>Weather Alert:</b> ${ac.stormAdvisorySummary ?: ""}<br/>\n")
            }
            sb.append("      ]]></description>\n")
            sb.append("      <Point>\n")
            sb.append("        <altitudeMode>absolute</altitudeMode>\n")
            sb.append("        <coordinates>${String.format(Locale.US, "%.5f", ac.longitude)},${String.format(Locale.US, "%.5f", ac.latitude)},$altMeters</coordinates>\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>\n")
        return sb.toString()
    }

    /**
     * Saves the KML file to the application's cache directory and triggers an Android Share Sheet
     * with MIME type "application/vnd.google-earth.kml+xml" to open directly in Google Earth or GIS apps.
     */
    fun exportAndShareKml(
        context: Context,
        kmlContent: String,
        fileName: String,
        chooserTitle: String = "Export KML to Google Earth"
    ) {
        try {
            val cacheDir = File(context.cacheDir, "kml_exports").apply { mkdirs() }
            val cleanName = if (fileName.endsWith(".kml", ignoreCase = true)) fileName else "$fileName.kml"
            val kmlFile = File(cacheDir, cleanName)

            FileWriter(kmlFile).use { writer ->
                writer.write(kmlContent)
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                kmlFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.google-earth.kml+xml"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, cleanName)
                putExtra(Intent.EXTRA_TEXT, "Exported 3D ADS-B flight telemetry from SkyWatch for Google Earth.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Toast.makeText(context, "KML generated: $cleanName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies the raw KML XML string to the system clipboard.
     */
    fun copyKmlToClipboard(context: Context, kmlContent: String, label: String = "SkyWatch KML") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, kmlContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "KML XML copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
