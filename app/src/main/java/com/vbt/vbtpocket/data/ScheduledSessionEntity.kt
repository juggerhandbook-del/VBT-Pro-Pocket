package com.vbt.vbtpocket.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_sessions")
data class ScheduledSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val workoutPlanId: Int,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val rule: String? = null,
    val notes: String? = null // <--- NUEVO CAMPO
)