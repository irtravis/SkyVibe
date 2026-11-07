package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lifecycle-aware composable providing smoothed real-time phone heading/azimuth (0°–359°),
 * allowing map rotation to track physical Earth North like Google Maps.
 */
@Composable
fun rememberDeviceHeading(): State<Float> {
    val context = LocalContext.current
    val heading = remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            private var lastHeading = 0f

            override fun onSensorChanged(event: SensorEvent) {
                var azimuth = 0f
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    azimuth = event.values[0]
                }

                val normalized = (azimuth + 360f) % 360f

                // Low-pass exponential smoothing filter for fluid compass rendering
                if (Math.abs(normalized - lastHeading) > 180f) {
                    lastHeading = normalized
                } else {
                    lastHeading += 0.2f * (normalized - lastHeading)
                }
                heading.floatValue = (lastHeading + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return heading
}
