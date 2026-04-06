package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "athlete_profile")
data class AthleteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, val weight: Float, val height: Int, val age: Int = 0,
    val level: String = "Intermedio", val squatRM: Float = 0f, val benchRM: Float = 0f, val deadliftRM: Float = 0f
)