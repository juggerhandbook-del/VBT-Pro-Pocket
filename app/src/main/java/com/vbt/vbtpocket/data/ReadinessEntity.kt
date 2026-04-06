package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "readiness_logs")
data class ReadinessEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val sleep: Int, val stress: Int, val fatigue: Int, val soreness: Int
)