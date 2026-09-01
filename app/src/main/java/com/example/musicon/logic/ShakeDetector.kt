package com.example.musicon.logic

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 12.0f
    private val SHAKE_INTERVAL_MS = 1000

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            if (gForce > SHAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > SHAKE_INTERVAL_MS) {
                    lastShakeTime = now
                    onShake()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
