package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, val description: String, val dateCreated: Long = System.currentTimeMillis()
)