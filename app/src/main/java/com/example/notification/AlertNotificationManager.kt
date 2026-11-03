package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.Aircraft

class AlertNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "skywatch_aircraft_alarm"
        const val CHANNEL_NAME = "Aircraft Proximity Watchlist Alarms"
        const val NOTIFICATION_ID_BASE = 8800
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when watchlisted tail numbers breach defined radar radius"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 500)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendWatchlistBreachAlert(aircraft: Aircraft, radiusNm: Double, locationName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_TAIL", aircraft.tailNumber)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            aircraft.tailNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val distFormatted = String.format("%.1f", aircraft.distanceNm)
        val title = "🚨 ALARM: ${aircraft.tailNumber} In Proximity!"
        val body = "${aircraft.registeredOperator} (${aircraft.callsign}) is ${distFormatted} NM away from $locationName at ${aircraft.formattedAltitude}."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nGround Speed: ${aircraft.velocityKts} kts · Heading: ${aircraft.headingDeg.toInt()}° · Type: ${aircraft.aircraftTypeName}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 500))
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE + (aircraft.tailNumber.hashCode() % 1000), notification)
        } catch (_: SecurityException) {
            // Handled when user hasn't granted POST_NOTIFICATIONS
        }
    }
}
