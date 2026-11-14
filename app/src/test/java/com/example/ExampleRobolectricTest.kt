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

    @Test
    fun `readsb v2 parser excludes landed stale ground vehicles and preserves helicopters`() {
        val json = """
            {
              "now": 1700000000.0,
              "ac": [
                {
                  "hex": "a40694",
                  "flight": "DAL2472 ",
                  "r": "N359DN",
                  "t": "A321",
                  "desc": "AIRBUS A321-211",
                  "ownOp": "DELTA AIR LINES INC",
                  "category": "A3",
                  "alt_baro": 33000,
                  "gs": 462.5,
                  "track": 221.0,
                  "baro_rate": 0,
                  "squawk": "2341",
                  "lat": 36.48,
                  "lon": -82.40,
                  "seen": 1.2,
                  "seen_pos": 2.4
                },
                {
                  "hex": "a9828a",
                  "flight": "MEDFLT1",
                  "r": "N71VA",
                  "t": "EC35",
                  "desc": "EUROCOPTER EC135",
                  "ownOp": "MED-FLIGHT",
                  "category": "A7",
                  "alt_baro": 2100,
                  "gs": 118.0,
                  "track": 95.0,
                  "lat": 36.52,
                  "lon": -82.38,
                  "seen": 2.0,
                  "seen_pos": 3.0
                },
                {
                  "hex": "a11111",
                  "flight": "LANDED1",
                  "r": "N111LL",
                  "t": "B738",
                  "category": "A3",
                  "alt_baro": "ground",
                  "gs": 12.0,
                  "lat": 36.47,
                  "lon": -82.41,
                  "seen": 1.0,
                  "seen_pos": 1.0
                },
                {
                  "hex": "a22222",
                  "flight": "STALE99",
                  "r": "N222ST",
                  "t": "C172",
                  "category": "A1",
                  "alt_baro": 3500,
                  "gs": 105.0,
                  "lat": 36.49,
                  "lon": -82.39,
                  "seen": 5.0,
                  "seen_pos": 58.0
                },
                {
                  "hex": "a33333",
                  "flight": "TRUCK1",
                  "category": "C1",
                  "alt_baro": 1500,
                  "gs": 25.0,
                  "lat": 36.47,
                  "lon": -82.41,
                  "seen": 1.0,
                  "seen_pos": 1.0
                },
                {
                  "hex": "a44444",
                  "flight": "PARKED1",
                  "alt_baro": 25,
                  "gs": 4.0,
                  "lat": 36.47,
                  "lon": -82.41,
                  "seen": 1.0,
                  "seen_pos": 1.0
                }
              ]
            }
        """.trimIndent()

        val targets = com.example.data.AdsBApiService.parseReadsbV2Json(json, "adsb.lol")
        assertEquals(2, targets.size)

        val delta = targets[0]
        assertEquals("a40694", delta.icao24)
        assertEquals("DAL2472", delta.callsign)
        assertEquals("N359DN", delta.registration)
        assertEquals("A321", delta.icaoTypeCode)
        assertEquals("DELTA AIR LINES INC", delta.registeredOwner)
        assertEquals(33000, delta.altitudeFt)

        val heli = targets[1]
        assertEquals("a9828a", heli.icao24)
        assertEquals("A7", heli.category)
        assertEquals("EC35", heli.icaoTypeCode)
        assertEquals(2100, heli.altitudeFt)
    }

    @Test
    fun `opensky fallback parser excludes on_ground and stale contacts`() {
        val nowSec = 1700000000L
        val openSkyJson = """
            {
              "time": $nowSec,
              "states": [
                ["a40694", "DAL2472 ", "United States", ${nowSec - 4}, ${nowSec - 3}, -82.40, 36.48, 10058.4, false, 235.0, 220.0, 0.0, null, 10100.0, "2341", false, 0, 4],
                ["a11111", "ONGRND1 ", "United States", ${nowSec - 2}, ${nowSec - 2}, -82.40, 36.48, 450.0, true, 5.0, 90.0, 0.0, null, 450.0, "1200", false, 0, 4],
                ["a22222", "OLDCON1 ", "United States", ${nowSec - 90}, ${nowSec - 85}, -82.41, 36.49, 4500.0, false, 180.0, 120.0, 0.0, null, 4500.0, "1200", false, 0, 3]
              ]
            }
        """.trimIndent()

        val parsed = com.example.data.AdsBApiService.parseOpenSkyJson(openSkyJson, nowSec)
        assertEquals(1, parsed.size)
        assertEquals("a40694", parsed[0].icao24)
        assertEquals("DAL2472", parsed[0].callsign)
        org.junit.Assert.assertFalse(parsed[0].onGround)
    }
}
