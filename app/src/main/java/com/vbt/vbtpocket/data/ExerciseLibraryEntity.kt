package com.vbt.vbtpocket.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "exercise_library")
data class ExerciseLibraryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, val muscleGroup: String, val description: String,
    val videoUrl: String = "", val imageUrl: String = ""
)