package com.vbt.vbtpocket.engine

// Representa 1 muestra exacta (32 bytes) enviada por el ESP32
data class IMUFrame(
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float,
    val mag: Float,
    val timestamp: Long
)