package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.SkyWatchRepository

class SkyWatchWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "SkyWatchWidget"
        const val ACTION_TOGGLE_PAUSE = "com.example.skywatch.ACTION_TOGGLE_PAUSE"
        const val ACTION_TOGGLE_VIEW = "com.example.skywatch.ACTION_TOGGLE_VIEW"
        const val ACTION_REFRESH = "com.example.skywatch.ACTION_REFRESH"

        fun sendRefreshBroadcast(context: Context) {
            try {
                val intent = Intent(context, SkyWatchWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send refresh broadcast", e)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            SkyWatchRepository.initWidgetDefaultCache()
        } catch (e: Exception) {
            Log.e(TAG, "Failed onEnabled cache init", e)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            SkyWatchRepository.initWidgetDefaultCache()
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onUpdate", e)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SkyWatchWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            when (intent.action) {
                ACTION_TOGGLE_PAUSE -> {
                    val prefs = context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)
                    val current = prefs.getBoolean("is_paused", false)
                    val newState = !current
                    prefs.edit().putBoolean("is_paused", newState).apply()
                    SkyWatchRepository.cachedIsPaused = newState

                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_aircraft_list)
                }
                ACTION_REFRESH -> {
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_aircraft_list)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling broadcast in onReceive", e)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        try {
            val prefs = context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)
            val isPaused = prefs.getBoolean("is_paused", false)
            val aircraft = SkyWatchRepository.cachedAircraftForWidget
            val alertCount = aircraft.count { it.isWatchlisted }

            val views = RemoteViews(context.packageName, R.layout.widget_skywatch_layout)

            // Status indicator
            if (isPaused) {
                views.setTextViewText(R.id.tv_widget_status, "PAUSED · RADIO SLEEP")
                views.setTextColor(R.id.tv_widget_status, 0xFFFFA000.toInt()) // Amber
                views.setTextViewText(R.id.btn_widget_pause, "▶ RESUME")
            } else {
                val countText = if (aircraft.isNotEmpty()) "${aircraft.size} IN RADAR" else "SCANNING"
                views.setTextViewText(R.id.tv_widget_status, "LIVE · $countText")
                views.setTextColor(R.id.tv_widget_status, 0xFF00E5FF.toInt()) // Cyan
                views.setTextViewText(R.id.btn_widget_pause, "⏸ PAUSE")
            }

            views.setTextViewText(
                R.id.tv_widget_radar_info,
                "RANGE: 50 NM · $alertCount WATCHLIST ALERTS"
            )

            // PendingIntent for Pause Button
            val pauseIntent = Intent(context, SkyWatchWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.btn_widget_pause, pausePendingIntent)

            // PendingIntent for Refresh Button
            val refreshIntent = Intent(context, SkyWatchWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            // PendingIntent for View Toggle Button (Opens App into Map vs List)
            val toggleViewIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TOGGLE_VIEW", true)
            }
            val toggleViewPendingIntent = PendingIntent.getActivity(
                context,
                1003,
                toggleViewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            views.setOnClickPendingIntent(R.id.btn_widget_view_toggle, toggleViewPendingIntent)

            // RemoteViewsService adapter for ListView
            val serviceIntent = Intent(context, SkyWatchWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("content://com.example.skywatch/widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_aircraft_list, serviceIntent)
            views.setEmptyView(R.id.widget_aircraft_list, R.id.widget_empty_view)

            // Row Click Template (Opens target aircraft in MainActivity)
            val rowClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val rowClickPendingIntent = PendingIntent.getActivity(
                context,
                1004,
                rowClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0)
            )
            views.setPendingIntentTemplate(R.id.widget_aircraft_list, rowClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app widget $appWidgetId", e)
        }
    }
}
