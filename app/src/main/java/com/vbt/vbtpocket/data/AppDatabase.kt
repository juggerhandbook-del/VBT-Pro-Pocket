package com.vbt.vbtpocket.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vbt.vbtpocket.data.dao.AthleteDao
import com.vbt.vbtpocket.data.dao.WorkoutDao

@Database(
    entities = [
        AthleteEntity::class,
        WorkoutPlanEntity::class,
        ExerciseEntity::class,
        ScheduledSessionEntity::class,
        ExerciseLibraryEntity::class,
        ReadinessEntity::class,
        WorkoutLogEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun athleteDao(): AthleteDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vbt_pocket_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}