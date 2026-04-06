package com.vbt.vbtpocket.engine

import kotlin.math.abs
import kotlin.math.sqrt

class VbtPhysicsEngine {
    private var gravityVector = floatArrayOf(0f, 0f, 1f)
    var isCalibrated = false
        private set

    private var velocity = 0f
    private var lastTimestamp = 0L
    private val alpha = 0.15f
    private var filteredAcc = 0f

    private var isRepInProgress = false
    private var maxVelocityInRep = 0f

    fun calibrateGravity(frames: List<IMUFrame>) {
        if (frames.size < 5) return
        gravityVector[0] = frames.map { it.ax }.average().toFloat()
        gravityVector[1] = frames.map { it.ay }.average().toFloat()
        gravityVector[2] = frames.map { it.az }.average().toFloat()
        isCalibrated = true
    }

    fun processFrame(frame: IMUFrame): Float? {
        if (!isCalibrated) return null

        val dotProduct = (frame.ax * gravityVector[0] + frame.ay * gravityVector[1] + frame.az * gravityVector[2])
        val gravityMag = sqrt(gravityVector[0] * gravityVector[0] + gravityVector[1] * gravityVector[1] + gravityVector[2] * gravityVector[2])
        val rawAccVertical = (dotProduct / gravityMag) - gravityMag

        filteredAcc = filteredAcc + alpha * (rawAccVertical - filteredAcc)

        if (lastTimestamp != 0L) {
            val dt = (frame.timestamp - lastTimestamp) / 1000f
            // Umbral de movimiento: 0.05G para empezar a integrar
            if (abs(filteredAcc) > 0.05f) {
                velocity += (filteredAcc * 9.81f) * dt
            } else {
                velocity *= 0.7f // ZUPT (Zero Velocity Update) más agresivo
            }
        }
        if (velocity < 0.01f) velocity = 0f
        lastTimestamp = frame.timestamp

        // DETECCIÓN DE REPETICIÓN
        if (velocity > 0.25f && !isRepInProgress) {
            isRepInProgress = true
            maxVelocityInRep = 0f
        }

        if (isRepInProgress) {
            if (velocity > maxVelocityInRep) maxVelocityInRep = velocity

            // Si la velocidad vuelve a "cero" (umbral 0.1)
            if (velocity < 0.10f) {
                isRepInProgress = false
                val finalVel = maxVelocityInRep
                maxVelocityInRep = 0f
                if (finalVel > 0.30f) return finalVel
            }
        }
        return null
    }

    fun reset() {
        velocity = 0f
        lastTimestamp = 0L
        filteredAcc = 0f
        isRepInProgress = false
        maxVelocityInRep = 0f
    }
}