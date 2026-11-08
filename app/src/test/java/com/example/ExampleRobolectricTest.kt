package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.example.widget.SkyWatchWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SkyWatch ADS-B", appName)
    }

    @Test
    fun `widget layout inflates successfully in RemoteViews without illegal views`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_skywatch_layout)
        val inflatedView: View = remoteViews.apply(context, null)
        assertNotNull("Widget root view must inflate without exception", inflatedView)
    }

    @Test
    fun `widget aircraft item layout inflates successfully in RemoteViews`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_aircraft_item)
        val inflatedView: View = remoteViews.apply(context, null)
        assertNotNull("Widget aircraft item must inflate without exception", inflatedView)
    }

    @Test
    fun `widget provider onUpdate runs without exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = SkyWatchWidgetProvider()
        provider.onUpdate(context, appWidgetManager, intArrayOf(101))
    }
}
