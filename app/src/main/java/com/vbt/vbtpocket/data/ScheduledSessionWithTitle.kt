package com.vbt.vbtpocket.data

import androidx.room.Embedded

// Esta data class combina los datos de una sesión programada con el título de la rutina
data class ScheduledSessionWithTitle(
    @Embedded val session: ScheduledSessionEntity,
    val workoutTitle: String
)