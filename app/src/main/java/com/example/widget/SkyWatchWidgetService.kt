package com.example.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.SkyWatchRepository
import com.example.model.Aircraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SkyWatchWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AircraftListRemoteViewsFactory(applicationContext)
    }
}

class AircraftListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Aircraft> = emptyList()
    private val timeFormatter = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate() {
        if (SkyWatchRepository.cachedAircraftForWidget.isEmpty()) {
            SkyWatchRepository.initWidgetDefaultCache()
        }
        items = SkyWatchRepository.cachedAircraftForWidget
    }

    override fun onDataSetChanged() {
        if (SkyWatchRepository.cachedAircraftForWidget.isEmpty()) {
            SkyWatchRepository.initWidgetDefaultCache()
        }
        items = SkyWatchRepository.cachedAircraftForWidget
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val aircraft = items[position]

        val views = RemoteViews(context.packageName, R.layout.widget_aircraft_item)

        // Identification
        val idText = if (aircraft.callsign.isNotBlank() && aircraft.callsign != aircraft.tailNumber) {
            "${aircraft.callsign} · ${aircraft.tailNumber}"
        } else {
            aircraft.tailNumber
        }
        views.setTextViewText(R.id.tv_item_callsign, idText)

        // Watchlist indicator
        if (aircraft.isWatchlisted) {
            views.setViewVisibility(R.id.tv_item_alert, View.VISIBLE)
            views.setTextViewText(R.id.tv_item_alert, "🚨 ALERT")
        } else {
            views.setViewVisibility(R.id.tv_item_alert, View.GONE)
        }

        // Distance
        views.setTextViewText(R.id.tv_item_dist, String.format(Locale.US, "%.1f NM", aircraft.distanceNm))

        // Registered Operator / Company
        views.setTextViewText(R.id.tv_item_operator, aircraft.registeredOperator)

        // Timestamp
        val timeString = timeFormatter.format(Date(aircraft.lastContactTimestamp))
        views.setTextViewText(R.id.tv_item_time, timeString)

        // Telemetry & Type
        val telemetryText = "${aircraft.formattedAltitude} · ${aircraft.velocityKts} kts · ${aircraft.headingDeg.toInt()}° · ${aircraft.aircraftTypeCode}"
        views.setTextViewText(R.id.tv_item_telemetry, telemetryText)

        // Fill-in intent for item click
        val fillInIntent = Intent().apply {
            putExtra("EXTRA_TARGET_TAIL", aircraft.tailNumber)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
