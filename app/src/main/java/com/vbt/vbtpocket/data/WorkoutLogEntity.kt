package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int, val exerciseName: String, val setNumber: Int,
    val weight: Float, val reps: Int, val rpe: Int, val velocity: Float = 0f
)