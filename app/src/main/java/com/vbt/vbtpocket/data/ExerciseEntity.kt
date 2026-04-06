package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutId: Int, val name: String, val targetSets: Int, val restSeconds: Int = 90,
    val restNextExerciseSeconds: Int = 120, val isVBT: Boolean = true, val targetVelocity: Float = 0f,
    val velocityLoss: Int = 20, val targetReps: Int = 0
)